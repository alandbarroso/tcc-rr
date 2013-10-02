package utilities;

public class Timer implements ITimeElapsed {
	private int startTime = 0;
	private int finishTime = 0;
	private boolean paused = false;

	public Timer() {
		reset();
	}

	public int getElapsedTime() {
		if (paused == false)
			return getMiliSecond() - startTime;
		return finishTime - startTime;
	}

	public void pause() {
		finishTime = getMiliSecond();
		paused = true;
	}

	public void reset() {
		paused = false;
		startTime = getMiliSecond();
	}

	private int getMiliSecond() {
		return (int) (System.nanoTime() / 1000000L);
	}

	public void setTime(int time) {
		if (paused)
			finishTime = startTime + time;
		else
			startTime = getMiliSecond() - time;
	}
}
