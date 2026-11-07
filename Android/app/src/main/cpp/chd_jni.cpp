#include <jni.h>
#include <libchdr/cdrom.h>
#include <libchdr/chd.h>
#include <unistd.h>

#include <algorithm>
#include <cstdint>
#include <cstdio>
#include <cstring>
#include <memory>
#include <new>

namespace {
struct Reader {
    explicit Reader(FILE *file) : file(file) {}
    ~Reader() {
        if (chd) chd_close(chd);
        fclose(file);
    }

    FILE *file;
    chd_file *chd = nullptr;
    std::unique_ptr<uint8_t[]> hunk;
    uint32_t frames = 0;
    uint32_t framesPerHunk = 0;
    uint32_t cachedHunk = UINT32_MAX;
};

bool validGeometry(FILE *file, const chd_header &header) {
    if (header.version != 5 || header.unitbytes != CD_FRAME_SIZE ||
        header.hunkbytes == 0 || header.hunkbytes > 1024 * 1024 ||
        header.hunkbytes % CD_FRAME_SIZE != 0 ||
        header.logicalbytes == 0 || header.logicalbytes > 1024ull * 1024 * 1024 ||
        header.logicalbytes % CD_FRAME_SIZE != 0) return false;

    if (header.compression[0] == CHD_CODEC_NONE) return true;

    // Limit the compressed map allocation before libchdr opens it.
    if (header.mapoffset > INT64_MAX || fseeko(file, static_cast<off_t>(header.mapoffset), SEEK_SET) != 0)
        return false;
    uint8_t length[4];
    if (fread(length, 1, sizeof(length), file) != sizeof(length)) return false;
    uint64_t mapBytes = 0;
    for (uint8_t byte : length) mapBytes = (mapBytes << 8) | byte;
    const uint64_t hunks = (header.logicalbytes + header.hunkbytes - 1) / header.hunkbytes;
    return mapBytes > 0 && mapBytes <= 4096 + 16 * hunks;
}

bool validSelfReferences(const chd_header &header) {
    if (header.compression[0] == CHD_CODEC_NONE) return true;
    if (header.mapentrybytes != 12 || !header.rawmap) return false;

    // libchdr follows compressed v5 map self-references recursively.
    auto depth = std::unique_ptr<uint8_t[]>(new (std::nothrow) uint8_t[header.hunkcount]{});
    if (!depth) return false;
    for (uint32_t hunk = 0; hunk < header.hunkcount; ++hunk) {
        const uint8_t *entry = header.rawmap + hunk * 12;
        if (entry[0] != 5) continue;  // COMPRESSION_SELF in the decoded v5 map.
        uint64_t target = 0;
        for (int i = 4; i < 10; ++i) target = (target << 8) | entry[i];
        if (target >= hunk || depth[target] >= 64) return false;
        depth[hunk] = depth[target] + 1;
    }
    return true;
}

uint32_t trackFrames(chd_file *chd, uint64_t units) {
    char text[128];
    uint32_t length;
    if (chd_get_metadata(chd, CDROM_TRACK_METADATA2_TAG, 0, text, sizeof(text), &length, nullptr, nullptr)
        != CHDERR_NONE || length == 0 || length > sizeof(text) || text[length - 1] != '\0')
        return 0;

    unsigned track, frames, pregap, postgap;
    char type[32], subtype[32], gapType[32], gapSubtype[32];
    int end = 0;
    if (sscanf(text, "TRACK:%9u TYPE:%31s SUBTYPE:%31s FRAMES:%9u PREGAP:%9u PGTYPE:%31s PGSUB:%31s POSTGAP:%9u%n",
               &track, type, subtype, &frames, &pregap, gapType, gapSubtype, &postgap, &end) != 8 ||
        end != static_cast<int>(length) - 1 || track != 1 || strcmp(type, "MODE2_RAW") != 0 ||
        pregap != 0 || postgap != 0 || frames == 0 || frames > units || units - frames > 3)
        return 0;

    if (chd_get_metadata(chd, CDROM_TRACK_METADATA2_TAG, 1, nullptr, 0, nullptr, nullptr, nullptr)
        != CHDERR_METADATA_NOT_FOUND ||
        chd_get_metadata(chd, CDROM_TRACK_METADATA_TAG, 0, nullptr, 0, nullptr, nullptr, nullptr)
        != CHDERR_METADATA_NOT_FOUND)
        return 0;
    return frames;
}
}

extern "C" JNIEXPORT jlong JNICALL
Java_dev_cricamfe_digivice_setup_ChdNative_open(JNIEnv *, jobject, jint fd) {
    const int copy = dup(fd);
    if (copy < 0) return 0;
    FILE *file = fdopen(copy, "rb");
    if (!file) {
        close(copy);
        return 0;
    }
    auto reader = std::unique_ptr<Reader>(new (std::nothrow) Reader(file));
    if (!reader) {
        fclose(file);
        return 0;
    }
    chd_header header{};
    if (chd_read_header_file(file, &header) != CHDERR_NONE || !validGeometry(file, header) ||
        chd_open_file(file, CHD_OPEN_READ, nullptr, &reader->chd) != CHDERR_NONE)
        return 0;

    const chd_header *opened = chd_get_header(reader->chd);
    if (!opened || opened->version != header.version || opened->unitbytes != header.unitbytes ||
        opened->hunkbytes != header.hunkbytes || opened->logicalbytes != header.logicalbytes ||
        !validSelfReferences(*opened))
        return 0;

    reader->frames = trackFrames(reader->chd, opened->logicalbytes / CD_FRAME_SIZE);
    if (reader->frames == 0) return 0;
    reader->framesPerHunk = opened->hunkbytes / CD_FRAME_SIZE;
    reader->hunk.reset(new (std::nothrow) uint8_t[opened->hunkbytes]);
    return reader->hunk ? reinterpret_cast<jlong>(reader.release()) : 0;
}

extern "C" JNIEXPORT jlong JNICALL
Java_dev_cricamfe_digivice_setup_ChdNative_size(JNIEnv *, jobject, jlong handle) {
    return static_cast<jlong>(reinterpret_cast<Reader *>(handle)->frames) * CD_MAX_SECTOR_DATA;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_dev_cricamfe_digivice_setup_ChdNative_read(JNIEnv *env, jobject, jlong handle, jlong position,
                                              jbyteArray output, jint offset, jint length) {
    auto &reader = *reinterpret_cast<Reader *>(handle);
    while (length > 0) {
        const auto frame = static_cast<uint32_t>(position / CD_MAX_SECTOR_DATA);
        const auto hunk = frame / reader.framesPerHunk;
        if (reader.cachedHunk != hunk) {
            reader.cachedHunk = UINT32_MAX;
            if (chd_read(reader.chd, hunk, reader.hunk.get()) != CHDERR_NONE) return JNI_FALSE;
            reader.cachedHunk = hunk;
        }
        const int start = static_cast<int>(position % CD_MAX_SECTOR_DATA);
        const int count = std::min(length, CD_MAX_SECTOR_DATA - start);
        const auto inHunk = (frame % reader.framesPerHunk) * CD_FRAME_SIZE + start;
        env->SetByteArrayRegion(output, offset, count, reinterpret_cast<const jbyte *>(reader.hunk.get() + inHunk));
        if (env->ExceptionCheck()) return JNI_FALSE;
        position += count;
        offset += count;
        length -= count;
    }
    return JNI_TRUE;
}

extern "C" JNIEXPORT void JNICALL
Java_dev_cricamfe_digivice_setup_ChdNative_close(JNIEnv *, jobject, jlong handle) {
    delete reinterpret_cast<Reader *>(handle);
}
