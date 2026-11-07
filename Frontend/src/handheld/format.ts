/** A number with the decimal mark of the app language: `0.8` in English, `0,8` in Spanish. */
export function decimal(value: number, locale: string, digits = 1): string {
  return new Intl.NumberFormat(locale, {
    minimumFractionDigits: digits,
    maximumFractionDigits: digits,
    useGrouping: false,
  }).format(value);
}

/** What an element multiplies the damage by: `×2.2` in English, `×2,2` in Spanish. */
export function times(multiplier: number, locale: string): string {
  return `×${decimal(multiplier, locale)}`;
}

/** The part of a text that ends at a full stop, one sentence at a time (for texts that carry two or three). */
export function sentences(text: string): string[] {
  return text
    .split(/(?<=\.)\s+/)
    .map((sentence) => sentence.trim())
    .filter((sentence) => sentence.length > 0);
}
