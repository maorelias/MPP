package ex2;

import java.util.Random;

public class MyAtomic {

	private static MyAtomic MRMW_ATOMIC_REGISTER;
	private AtomicMRMWRegister atomicMRMWRegister;
	private static final int CHAR_VALUES = 256;
	private static final int LONG_SIZE = 64;

	public MyAtomic(int numberOfThreads) {
		this.atomicMRMWRegister = new AtomicMRMWRegister(numberOfThreads, (char) 0);
	}

	public void write(char c) {
		atomicMRMWRegister.write(c);
	}

	public char read() {
		return atomicMRMWRegister.read();
	}

	public static void main(String[] args) {
		int numOfThreads = Integer.parseInt(args[0]);
		StampedValue.MIN_VALUE = new StampedValue((char) 0, numOfThreads);
		MRMW_ATOMIC_REGISTER = new MyAtomic(numOfThreads);

		WorkerThread[] threads = new WorkerThread[numOfThreads];

		// spawn threads that do the required task
		for (int threadNumber = 0; threadNumber < numOfThreads; threadNumber++) {
			WorkerThread thread = new WorkerThread(threadNumber);
			threads[threadNumber] = thread;
		}

		// start all threads
		for (WorkerThread thread : threads) {
			thread.start();
		}

		// wait for all threads to finish
		for (WorkerThread thread : threads) {
			try {
				thread.join();
			} catch (InterruptedException e) {
			}
		}
	}

	static class WorkerThread extends Thread {

		private static final int OPERATION_COUNT = 1000000;
		private int threadId;

		public WorkerThread(int threadId) {
			super();
			this.threadId = threadId;
		}

		@Override
		public void run() {
			for (int i = 0; i < OPERATION_COUNT; i++) {
				Random rand = new Random();

				// perform a random operation with even chances
				boolean coin = rand.nextBoolean();
				if (coin) {
					char c = MRMW_ATOMIC_REGISTER.read();
				} else {
					// write a random character
					char c = (char) rand.nextInt(CHAR_VALUES);
					MRMW_ATOMIC_REGISTER.write(c);
				}
			}
		}

		public int getThreadId() {
			return threadId;
		}
	}

	static class SafeBoolSRSWRegister {

		private volatile boolean value = false;

		public void write(boolean b) {
			value = b;
		}

		public boolean read() {
			return value;
		}
	}

	static class SafeBoolMRSWRegister {
		private SafeBoolSRSWRegister[] r;
		private int readersNum;

		public SafeBoolMRSWRegister(int readersNum) {
			this.r = new SafeBoolSRSWRegister[readersNum];
			for (int i = 0; i < readersNum; i++) {
				r[i] = new SafeBoolSRSWRegister();
			}
			this.readersNum = readersNum;
		}

		public void write(boolean x) {
			for (int j = 0; j < readersNum; j++)
				r[j].write(x);
		}

		public boolean read() {
			int i = ((WorkerThread) Thread.currentThread()).getThreadId();
			return r[i].read();
		}
	}

	static class RegBoolMRSWRegister {
		private volatile boolean old;
		private SafeBoolMRSWRegister value;

		public RegBoolMRSWRegister(int readersNum) {
			value = new SafeBoolMRSWRegister(readersNum);
		}

		public void write(boolean x) {
			if (old != x) {
				value.write(x);
				old = x;
			}
		}

		public boolean read() {
			return value.read();
		}
	}

	static class RegMRSWRegister {
		private RegBoolMRSWRegister[] bit;
		private int numOfValues;

		public RegMRSWRegister(int numOfValues, int readersNum) {
			this.bit = new RegBoolMRSWRegister[numOfValues];
			for (int i = 0; i < this.bit.length; i++) {
				this.bit[i] = new RegBoolMRSWRegister(readersNum);
			}
			// default value is 0
			this.bit[0].write(true);
			this.numOfValues = numOfValues;
		}

		public void write(char x) {
			bit[x].write(true);
			for (int i = x - 1; i >= 0; i--)
				bit[i].write(false);
		}

		public char read() {
			for (int i = 0; i < numOfValues; i++) {
				if (bit[i].read()) {
					return (char) i;
				}
			}
			return 0;
		}
	}

	static class LongTimeStamp {

		// long number will be represented in binary base
		// representation will be stored in 64 boolean regular MRSW registers
		private volatile RegBoolMRSWRegister[] stamp;

		public LongTimeStamp(long stamp, int readersNum) {
			this.stamp = getNewStampRegister(stamp, readersNum);
		}

		private RegBoolMRSWRegister[] getNewStampRegister(long stamp, int readersNum) {
			RegBoolMRSWRegister[] register = new RegBoolMRSWRegister[LONG_SIZE];
			for (int i = 0; i < register.length; i++) {
				register[i] = new RegBoolMRSWRegister(readersNum);
			}
			// transform to binary representation
			String binaryRep = getBinaryString(stamp);
			for (int i = 0; i < LONG_SIZE; i++) {
				if (binaryRep.charAt(i) == '1') {
					register[i].write(true);
				}
			}
			return register;
		}

		private String getBinaryString(long stamp) {
			String binString = Long.toBinaryString(stamp);
			String fullBinaryRepresentationString = "";
			for (int i = 0; i < LONG_SIZE - binString.length(); i++) {
				fullBinaryRepresentationString += "0";
			}

			fullBinaryRepresentationString += binString;
			return fullBinaryRepresentationString;
		}

		public long getTimeStamp() {
			String binaryRepresentation = "";
			// restore binary representation from boolean registers
			for (int i = 0; i < LONG_SIZE; i++) {
				if (stamp[i].read() == true) {
					binaryRepresentation += "1";
				} else {
					binaryRepresentation += "0";
				}
			}

			// transform string binary representation into long
			long longTimeStamp = Long.parseLong(binaryRepresentation, 2);
			return longTimeStamp;
		}
	}

	static class StampedValue {
		public LongTimeStamp longStamp;
		public RegMRSWRegister value;

		public StampedValue(char init, int readersNum) {
			longStamp = new LongTimeStamp(0, readersNum);
			value = new RegMRSWRegister(CHAR_VALUES, readersNum);
			value.write(init);
		}

		public StampedValue(long stamp, char value, int readersNum) {
			this.longStamp = new LongTimeStamp(stamp, readersNum);
			this.value = new RegMRSWRegister(CHAR_VALUES, readersNum);
			this.value.write(value);
		}

		public static StampedValue max(StampedValue x, StampedValue y) {
			long x_stamp = x.getStamp();
			long y_stamp = y.getStamp();
			if (x_stamp > y_stamp) {
				return x;
			} else {
				return y;
			}
		}

		public long getStamp() {
			return longStamp.getTimeStamp();
		}

		public static StampedValue MIN_VALUE;
	}

	static class AtomicMRMWRegister {
		private StampedValue[] a_table;

		private int capacity;

		public AtomicMRMWRegister(int capacity, char init) {
			this.capacity = capacity;
			a_table = (StampedValue[]) new StampedValue[capacity];
			StampedValue value = new StampedValue(init, capacity);
			for (int j = 0; j < a_table.length; j++) {
				a_table[j] = value;
			}
		}

		public void write(char value) {
			int me = ((WorkerThread) Thread.currentThread()).getThreadId();
			StampedValue max = StampedValue.MIN_VALUE;
			for (int i = 0; i < a_table.length; i++) {
				max = StampedValue.max(max, a_table[i]);
			}
			a_table[me] = new StampedValue(max.getStamp() + 1, value, capacity);
		}

		public char read() {
			StampedValue max = StampedValue.MIN_VALUE;
			for (int i = 0; i < a_table.length; i++) {
				max = StampedValue.max(max, a_table[i]);
			}
			return max.value.read();
		}
	}
}
