package org.hermeslite.hardware;

import java.net.DatagramPacket;

import org.hermeslite.utility.RingBuffer;

/**
 * 
 * RX emulator.
 * 
 * 
 * Note : for actual hardware a separate RxHandler must be defined
 * 
 * @author PA3GSB
 *
 */
public class RxEmulatorHandler implements Runnable, IRxHandler {

	private final static int SAMPLES = 48000;
	private final static int BANDWIDTH = 48000;
	private final static double TIMESTEP = (1.0 / BANDWIDTH);
	private double timebase = 0.0;

	double amplitude;
	double noiseAmplitude;

	private int vfo = 14250000;
	private int frequency = 14260000;
	private volatile Thread thread;

	// private RingBuffer<Byte> rxIQBuffer;
	private RingBuffer<Byte> rxIQBuffer;

	Byte[] result = new Byte[6 * SAMPLES];

	boolean changed = true;

	public RxEmulatorHandler(RingBuffer<Byte> rxIQBuffer) {
		this.rxIQBuffer = rxIQBuffer;

		int amplitudeDB = -73;
		amplitude = 1 / Math.pow(Math.sqrt(10.0), -(double) amplitudeDB / 10.0);
		System.out.println("Output generator -73dBm (S9)  = " + amplitude);

		int amplitudeNoiseDB = -90;
		noiseAmplitude = 1 / Math.pow(Math.sqrt(10), -(double) amplitudeNoiseDB / 10);

		timebase = 0.0;
	}

	public void start() {
		if (thread == null) {
			thread = new Thread(this);
			thread.setPriority(Thread.MAX_PRIORITY);
			thread.start();
		}
	}

	public void stop() {
		while (rxIQBuffer.size() > 0) {
			try {
				rxIQBuffer.remove();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		thread = null;

	}

	@Override
	public void run() {
		while (thread == Thread.currentThread()) {
			if (rxIQBuffer.size() < SAMPLES * 6 * 2) // 48000 * 6 * 2
			{
				long start = System.nanoTime();
				if (changed) {
					generateIQ();
					
				} 
				
				try {
					rxIQBuffer.add(result);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				changed = false;
				double time = (System.nanoTime() - start) / 100000d;
				System.out.println("vullen gegener buffers " + time + " msec");
				
				System.out.println(rxIQBuffer.size());
			}
		}
	}

	private void generateIQ() {

		float[] inlsamples = new float[SAMPLES];
		float[] inrsamples = new float[SAMPLES];

		int f1 = (int) frequency - vfo;

		int idx = 0;

		if (f1 > 24000 || f1 < -24000) {
			amplitude = 0;
		} else
			amplitude = 1 / Math.pow(Math.sqrt(10.0), -(double) -73 / 10.0);

		while (idx < SAMPLES) {
			double angle1 = f1 * 2 * Math.PI * timebase;
			inlsamples[idx] = (float) (noiseAmplitude * (2 * Math.random() - 1) * 0x7fffff);
			inrsamples[idx] = (float) (noiseAmplitude * (2 * Math.random() - 1) * 0x7fffff);
			inlsamples[idx] += (float) (Math.sin(angle1) * 0x7fffff * amplitude);
			inrsamples[idx] += (float) (Math.cos(angle1) * 0x7fffff * amplitude);
			int q = Math.round(inrsamples[idx]); // * 0x7fffff
			int i = Math.round(inlsamples[idx]);// * 0x7fffff

			result[idx * 6] = ((byte) ((i >> 16) & 0xff));
			result[idx * 6 + 1] = ((byte) ((i >> 8) & 0xff));
			result[idx * 6 + 2] = ((byte) (i & 0xff));
			result[idx * 6 + 3] = ((byte) ((q >> 16) & 0xff));
			result[idx * 6 + 4] = ((byte) ((q >> 8) & 0xff));
			result[idx * 6 + 5] = ((byte) (q & 0xff));

			// try {
			// rxIQBuffer.add(i);
			// rxIQBuffer.add(q);
			// rxIQBuffer.add((byte) ((i >> 16) & 0xff));
			// rxIQBuffer.add((byte) ((i >> 8) & 0xff));
			// rxIQBuffer.add((byte) (i & 0xff ));
			// rxIQBuffer.add((byte) ((q >> 16) & 0xff));
			// rxIQBuffer.add((byte) ((q >> 8) & 0xff));
			// rxIQBuffer.add((byte)(q & 0xff));
			// } catch (InterruptedException e) {
			// System.out.println("adding problem");
			// }

			idx++;
			timebase += TIMESTEP;
		}

		
	}

	@Override
	public RingBuffer<Byte> getReceiveIQStream() {
		return rxIQBuffer;
	}

	@Override
	public void setRXFrequency(int freq) {
		vfo = freq;

		changed = true;
	}

	@Override
	public void setSamplingRate(int rate) {
		// not required yet with actual hardware...
	}

}
