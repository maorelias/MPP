package ex1;

public class Ex1q7nv {

	public static int COUNTER = 0;
	public final static int ITERATION_COUNT = 1000000;
	public static String THREAD_1_NAME = "0";
	public static String THREAD_2_NAME = "1";
	public static NonVolatilePetersonLock COUNTER_LOCK = new NonVolatilePetersonLock();

	public static void main(String[] args) {

		Thread thread1 = new Thread(new Runnable() {

			@Override
			public void run() {
				for (int i = 0; i < ITERATION_COUNT; i++) {
					COUNTER_LOCK.lock();
					try {
						int currentCounter = COUNTER;
						currentCounter++;
						COUNTER = currentCounter;
					} finally {
						COUNTER_LOCK.unlock();
					}
				}
			}
		});

		thread1.setName(THREAD_1_NAME);

		Thread thread2 = new Thread(new Runnable() {

			@Override
			public void run() {
				for (int i = 0; i < ITERATION_COUNT; i++) {
					COUNTER_LOCK.lock();
					try {
						int currentCounter = COUNTER;
						currentCounter++;
						COUNTER = currentCounter;
					} finally {
						COUNTER_LOCK.unlock();
					}
				}
			}
		});

		thread2.setName(THREAD_2_NAME);

		long startTime = System.currentTimeMillis();
		thread1.start();
		thread2.start();

		try {
			thread1.join();
		} catch (InterruptedException e) {
		}

		try {
			thread2.join();
		} catch (InterruptedException e) {
		}

		long finishTime = System.currentTimeMillis();
		long elapsedTime = finishTime - startTime;

		System.out.println("Counter: " + COUNTER);
		System.out.println("Elapsed time in ms: " + elapsedTime);
	}

	static class NonVolatilePetersonLock {

		private int victim;
		private boolean[] flag = new boolean[2];

		public void lock() {
			int i = Integer.parseInt(Thread.currentThread().getName());
			flag[i] = true;
			victim = i;
			while (flag[1 - i] && victim == i)
				;
		}

		public void unlock() {
			int i = Integer.parseInt(Thread.currentThread().getName());
			flag[i] = false;
		}
	}
}
