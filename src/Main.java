public class Main {
    public static void main(String[] args) {

        //create new thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                new Client("1").start();
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                new Client("2").start();
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                new Client("3").start();
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                new Client("4").start();
            }
        }).start();
    }
}
