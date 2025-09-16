package tabos

class Fraction extends Number {
    final int numerator
    final int denominator

    Fraction(int numerator, int denominator = 1) {
        if (denominator == 0) throw new IllegalArgumentException("Denominator cannot be zero")
        int gcd = BigInteger.valueOf(Math.abs(numerator)).gcd(BigInteger.valueOf(Math.abs(denominator))).intValue()
        this.numerator = ((denominator < 0 ? -numerator : numerator) / gcd) as int
        this.denominator = ((denominator < 0 ? -denominator : denominator) / gcd) as int
    }

    Fraction multiply(Fraction other) {
        new Fraction(numerator * other.numerator, denominator * other.denominator)
    }

    Fraction multiply(Number other) {
        new Fraction(numerator * other.intValue(), denominator)
    }

    Fraction div(Fraction other) {
        new Fraction(numerator * other.denominator, denominator * other.numerator)
    }

    Fraction div(Number other) {
        new Fraction(numerator, denominator * other.intValue())
    }

    Fraction plus(Fraction other) {
        new Fraction(
            numerator * other.denominator + other.numerator * denominator,
            denominator * other.denominator
        )
    }

    Fraction plus(Number other) {
        new Fraction(numerator + other.intValue() * denominator, denominator)
    }

    Fraction minus(Number other) {
        new Fraction(numerator - other.intValue() * denominator, denominator)
    }

    Fraction minus(Fraction other) {
        new Fraction(
            numerator * other.denominator - other.numerator * denominator,
            denominator * other.denominator
        )
    }

    Fraction negative() {
        new Fraction(-numerator, denominator)
    }

    Fraction power(int exponent) {
        if (exponent >= 0) {
            new Fraction(numerator ** exponent, denominator ** exponent)
        } else {
            new Fraction(denominator ** (-exponent), numerator ** (-exponent))
        }
    }

    boolean asBoolean() {
        numerator != 0
    }

    @Override double doubleValue() { (double) numerator / denominator }
    @Override float floatValue() { (float) doubleValue() }
    @Override int intValue() { (int) doubleValue() }
    @Override long longValue() { (long) doubleValue() }

    @Override
    String toString() {
        denominator == 1 ? "$numerator" : "$numerator/$denominator"
    }

    boolean equals(other) {
        if (!(other instanceof Fraction)) return false
        numerator == other.numerator && denominator == other.denominator
    }

    int hashCode() {
        Objects.hash(numerator, denominator)
    }
}

Number.metaClass.multiply << { Fraction f -> f * (delegate as Number) }
Number.metaClass.plus << { Fraction f -> f + (delegate as Number) }
Number.metaClass.minus << { Fraction f -> new Fraction((int) (delegate as Number) * f.denominator - f.numerator, f.denominator) }
Number.metaClass.div << { Fraction f -> new Fraction((int) (delegate as Number) * f.denominator, f.numerator) }