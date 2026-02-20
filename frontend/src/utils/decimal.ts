interface ParsedDecimal {
  sign: 1 | -1;
  digits: bigint;
  scale: number;
}

function pow10(exp: number): bigint {
  if (exp <= 0) {
    return 1n;
  }
  return 10n ** BigInt(exp);
}

type DecimalInput = string | number | bigint;

function toDecimalString(value: DecimalInput): string | null {
  if (typeof value === 'string') {
    return value;
  }
  if (typeof value === 'number') {
    if (!Number.isFinite(value)) {
      return null;
    }
    return value.toString();
  }
  return value.toString();
}

function parseDecimal(value: DecimalInput): ParsedDecimal | null {
  const text = toDecimalString(value);
  if (text == null) {
    return null;
  }
  const trimmed = text.trim();
  if (!trimmed) {
    return null;
  }

  const match = trimmed.match(/^([+-])?(\d+)(?:\.(\d+))?$/);
  if (!match) {
    return null;
  }

  const [, signToken, integerPartRaw, fractionPartRaw = ''] = match;
  const integerPart = integerPartRaw.replace(/^0+(?=\d)/, '');
  const fractionPart = fractionPartRaw.replace(/0+$/, '');
  const scale = fractionPart.length;
  const digitsString = `${integerPart}${fractionPart}`.replace(/^0+$/, '0');
  const digits = BigInt(digitsString);
  const sign: 1 | -1 = signToken === '-' ? -1 : 1;

  if (digits === 0n) {
    return { sign: 1, digits: 0n, scale: 0 };
  }

  return { sign, digits, scale };
}

function formatDecimal(value: bigint, scale: number): string {
  const sign = value < 0n ? '-' : '';
  let digits = (value < 0n ? -value : value).toString();

  if (scale > 0) {
    if (digits.length <= scale) {
      digits = `${'0'.repeat(scale - digits.length + 1)}${digits}`;
    }
    const split = digits.length - scale;
    const integerPart = digits.slice(0, split);
    const fractionPart = digits.slice(split).replace(/0+$/, '');
    return fractionPart ? `${sign}${integerPart}.${fractionPart}` : `${sign}${integerPart}`;
  }

  return `${sign}${digits}`;
}

function toScaledInt(value: ParsedDecimal, targetScale: number): bigint {
  if (targetScale <= value.scale) {
    return BigInt(value.sign) * value.digits / pow10(value.scale - targetScale);
  }
  return BigInt(value.sign) * value.digits * pow10(targetScale - value.scale);
}

export function normalizeDecimal(value: DecimalInput): string | null {
  const parsed = parseDecimal(value);
  if (!parsed) {
    return null;
  }
  return formatDecimal(BigInt(parsed.sign) * parsed.digits, parsed.scale);
}

export function isPositiveDecimal(value: DecimalInput): boolean {
  const parsed = parseDecimal(value);
  return parsed !== null && parsed.digits > 0n && parsed.sign > 0;
}

export function multiplyDecimal(a: DecimalInput, b: DecimalInput): string | null {
  const left = parseDecimal(a);
  const right = parseDecimal(b);
  if (!left || !right) {
    return null;
  }
  const resultSign = left.sign * right.sign;
  const resultDigits = left.digits * right.digits;
  const resultScale = left.scale + right.scale;
  return formatDecimal(BigInt(resultSign) * resultDigits, resultScale);
}

export function divideDecimal(a: DecimalInput, b: DecimalInput, scale = 18): string | null {
  const left = parseDecimal(a);
  const right = parseDecimal(b);
  if (!left || !right || right.digits === 0n) {
    return null;
  }

  const exp = right.scale + scale - left.scale;
  const numerator = left.digits * (exp >= 0 ? pow10(exp) : 1n);
  const denominator = right.digits * (exp < 0 ? pow10(-exp) : 1n);

  const quotient = numerator / denominator;
  const remainder = numerator % denominator;
  const rounded = remainder * 2n >= denominator ? quotient + 1n : quotient;
  const resultSign = left.sign * right.sign;

  return formatDecimal(BigInt(resultSign) * rounded, scale);
}

export function decimalToFractionalPercent(percent: DecimalInput): string | null {
  return divideDecimal(percent, '100', 18);
}

export function fractionalToPercent(value: DecimalInput): string | null {
  return multiplyDecimal(value, '100');
}

export function decimalToDisplay(value: DecimalInput, maxScale = 18): string | null {
  const parsed = parseDecimal(value);
  if (!parsed) {
    return null;
  }
  if (parsed.scale <= maxScale) {
    return formatDecimal(BigInt(parsed.sign) * parsed.digits, parsed.scale);
  }
  const scaled = toScaledInt(parsed, maxScale);
  return formatDecimal(scaled, maxScale);
}
