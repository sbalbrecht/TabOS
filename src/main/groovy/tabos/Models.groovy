package tabos

import groovy.transform.TupleConstructor

@TupleConstructor
class TrackSettings {
    boolean tablature = true
    boolean notation = true
    boolean diagramsAreBelow = false
    boolean showRhythm = false
    boolean forceHorizontal = false
    boolean forceChannels = false
    boolean diagramList = true
    boolean diagramsInScore = false
    boolean autoLetRing = false
    boolean autoBrush = false
    boolean extendRhythmic = false
}

@TupleConstructor
class RSEEqualizer {
    List<Float> knobs = []
    Float gain = 0.0f
}

class RSEInstrument {
    int instrument = -1
    int unknown = -1
    int soundBank = -1
    int effectNumber = -1
    String effectCategory = ''
    String effect = ''
}

class TrackRSE {
    RSEInstrument instrument
    RSEEqualizer equalizer
    int humanize = 0
    Accentuation autoAccentuation = Accentuation.NONE
//    def __attrs_post_init__(self)
//        if not self. equalizer.knobs:
//        self.equalizer.knobs = [ 0.0 ] * 3
}

enum TripletFeel {
    NONE(0),
    EIGHTH(1),
    SIXTEENTH(2),
    final int value
    TripletFeel(int value) { this.value = value }
    static from(int value) { values().find{ it.value == value } }
}

enum Accentuation {
    NONE(0),
    VERY_SOFT(1),
    SOFT(2),
    MEDIUM(3),
    STRONG(4),
    VERY_STRONG(5),
    final int value
    Accentuation(int value) { this.value = value }
    static from(int value) { values().find{ it.value == value } }
}

record GuitarString(int number, int value) {
    @Override String toString() { 'C C# D D# E F F# G G# A A# B'.split()[value % 12] + value.intdiv(12) }
}

enum MeasureClef {
    TREBLE(0),
    BASS(1),
    TENOR(2),
    ALTO(3),
    final int value
    MeasureClef(int value) { this.value = value }
    static from(int value) { values().find{ it.value == value } }
}

enum KeySignature {
    F_MAJOR_FLAT(-8, 0),
    C_MAJOR_FLAT(-7, 0),
    G_MAJOR_FLAT(-6, 0),
    D_MAJOR_FLAT(-5, 0),
    A_MAJOR_FLAT(-4, 0),
    E_MAJOR_FLAT(-3, 0),
    B_MAJOR_FLAT(-2, 0),
    F_MAJOR(-1, 0),
    C_MAJOR(0, 0),
    G_MAJOR(1, 0),
    D_MAJOR(2, 0),
    A_MAJOR(3, 0),
    E_MAJOR(4, 0),
    B_MAJOR(5, 0),
    F_MAJOR_SHARP(6, 0),
    C_MAJOR_SHARP(7, 0),
    G_MAJOR_SHARP(8, 0),

    D_MINOR_FLAT(-8, 1),
    A_MINOR_FLAT(-7, 1),
    E_MINOR_FLAT(-6, 1),
    B_MINOR_FLAT(-5, 1),
    F_MINOR(-4, 1),
    C_MINOR(-3, 1),
    G_MINOR(-2, 1),
    D_MINOR(-1, 1),
    A_MINOR(0, 1),
    E_MINOR(1, 1),
    B_MINOR(2, 1),
    F_MINOR_SHARP(3, 1),
    C_MINOR_SHARP(4, 1),
    G_MINOR_SHARP(5, 1),
    D_MINOR_SHARP(6, 1),
    A_MINOR_SHARP(7, 1),
    E_MINOR_SHARP(8, 1),
    final int value
    final int isMinor
    KeySignature(int value, int isMinor) {
        this.value = value
        this.isMinor = isMinor
    }
    static from(int value, int isMinor) { values().find{ it.value == value && it.isMinor == isMinor } }
}

enum BeatStrokeDirection {
    NONE(0),
    UP(1),
    DOWN(2),
    final int value
    BeatStrokeDirection(int value) { this.value = value }
    static from(int value) { values().find{ it.value == value } }
}

enum SlapEffect {
    NONE(0),
    TAPPING(1),
    SLAPPING(2),
    POPPING(3),
    final int value
    SlapEffect(int value) { this.value = value }
    static from(int value) { values().find{ it.value == value } }
}

enum SlideType {
    INTO_FROM_ABOVE(-2),
    NONE(0),
    SHIFT_SLIDE_TO(1),
    LEGATO_SLIDE_TO(2),
    OUT_DOWNWARDS(3),
    OUT_UPWARDS(4),
    final int value
    SlideType(int value) { this.value = value }
    static from(int value) { values().find{ it.value == value } }
}

enum Fingering {
    OPEN(-1),
    THUMB(0),
    INDEX(1),
    MIDDLE(2),
    ANNULAR(3),
    LITTLE(4),
    final int value
    Fingering(int value) { this.value = value }
    static from(int value) { values().find{ it.value == value } }
}

enum LineBreak {
    NONE(0),
    BREAK(1),
    PROTECT(2),
    final int value
    LineBreak(int value) { this.value = value }
    static from(int value) { values().find{ it.value == value } }
}

enum VoiceDirection {
    NONE(0),
    UP(1),
    DOWN(2),
    final int value
    VoiceDirection(int value) { this.value = value }
    static from(int value) { values().find{ it.value == value } }
}

enum Octave {
    // loco = 0, 8va = 1, 15ma = 2   ??
    NONE(0),
    OTTAVA(1),
    QUINDICESIMA(2),
    OTTAVA_BASSA(3),
    QUINDICESIMA_BASSA(4),
    final int value
    Octave(int value) { this.value = value }
    static Octave from(int value) { values().find{ it.value == value } }
}

enum BeatStatus {
    EMPTY(0),
    NORMAL(1),
    REST(2),
    final int value
    BeatStatus(int value) { this.value = value }
    static BeatStatus from(int value) { values().find{ it.value == value } }
}

@TupleConstructor
abstract class HarmonicEffect {
    final int type
}

class NaturalHarmonic extends HarmonicEffect {
    NaturalHarmonic() { super(1) }
}

@TupleConstructor
class ArtificialHarmonic extends HarmonicEffect {
    Pitch pitch
    Octave octave
    ArtificialHarmonic() { super(2) }
}

@TupleConstructor
class TappedHarmonic extends HarmonicEffect {
    int fret
    TappedHarmonic() { super(3) }
}

class PinchHarmonic extends HarmonicEffect {
    PinchHarmonic() { super(4) }
}

class SemiHarmonic extends HarmonicEffect {
    SemiHarmonic() { super(5) }
}

class Pitch {
    enum Intonation {
        SHARP('C C# D D# E F F# G G# A A# B'.split()),
        FLAT('C Db D Eb E F Gb G Ab A Bb B'.split())
        List<String> semitones
        Intonation(semitones) { this.semitones = semitones }
    }
    /** C = 0, D = 2, E = 4, F = 5, ...*/
    int just
    /** b = -1, # = 1 */
    int accidental
    int value
    Intonation intonation
    PitchClass(int tone, int accidental, Intonation intonation = null) {
        this.just = tone % 12
        this.accidental = accidental
        this.value = (this.just + accidental) % 12
        this.intonation = intonation ?: (accidental == -1 ? Intonation.FLAT : Intonation.SHARP)
    }
    @Override String toString() { intonation.semitones[value] }
}

@TupleConstructor
class MixTableItem {
    int value = 0
    int duration = 0
    boolean allTracks = false
}

@TupleConstructor
class WahEffect {
    private static final OFF = new WahEffect(-2)
    private static final NONE = new WahEffect(-1)
    int value = -1
    boolean display = false
    def isOff() { value == OFF.value }
    def isNone() { value == NONE.value }
    def isOn() { value in (0..100) }
}

class MixTableChange {
    MixTableItem instrument
    RSEInstrument rse
    MixTableItem volume
    MixTableItem balance
    MixTableItem chorus
    MixTableItem reverb
    MixTableItem phaser
    MixTableItem tremolo
    String tempoName = ''
    MixTableItem tempo
    boolean hideTempo = true
    WahEffect wah
    boolean useRSE = false
}

enum GraceEffectTransition {
    NONE(0),
    SLIDE(1),
    BEND(2),
    HAMMER(3),
    final int value
    GraceEffectTransition(int value) { this.value = value }
    static GraceEffectTransition from(int value) { values().find{ it.value == value }}
}

class Velocities {
    static int minVelocity = 15
    static int velocityIncrement = 16
    static int pianoPianissimo = minVelocity
    static int pianissimo = minVelocity + velocityIncrement
    static int piano = minVelocity + velocityIncrement * 2
    static int mezzoPiano = minVelocity + velocityIncrement * 3
    static int mezzoForte = minVelocity + velocityIncrement * 4
    static int forte = minVelocity + velocityIncrement * 5
    static int fortissimo = minVelocity + velocityIncrement * 6
    static int forteFortissimo = minVelocity + velocityIncrement * 7
    static int defaultVelocity = forte
}

