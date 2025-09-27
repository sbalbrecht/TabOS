package tabos

class Main {
    static void main(String[] args) {
        InputStream is = new File(args[0]).newInputStream()
        new GP5InputStream(is).withCloseable {
            it.readSong()
        }
    }
}
