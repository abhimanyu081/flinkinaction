package com.manning.fia.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.tuple.Tuple;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.api.java.tuple.Tuple5;
import org.apache.flink.api.java.tuple.Tuple6;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.AssignerWithPeriodicWatermarks;
import org.apache.flink.streaming.api.functions.timestamps.AscendingTimestampExtractor;
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor;
import org.apache.flink.streaming.api.functions.windowing.WindowFunction;
import org.apache.flink.streaming.api.watermark.Watermark;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

/**
 * Created by hari on 6/26/16.
 */
public class SampleParallelSourceStreamingPipeline {

	public void executeJobOutOfOrderWithBoundedLateness(int parallelism, int noOfEvents, long standardDelayInSeconds,
			long anamolousDelayInSeconds, int watermarkEveryNInterval) throws Exception {
		Map<Integer, List<Event>> data = new HashMap<>();
		Map<Integer, Long> delay = new HashMap<>();
		for (int i = 0; i < (parallelism - 1); i++) {
			delay.put(i, standardDelayInSeconds);
		}
		delay.put((parallelism - 1), anamolousDelayInSeconds);
		for (int i = 0; i < parallelism; i++) {
			data.put(i, new ArrayList<Event>());
		}
		for (int i = 0; i < noOfEvents; i++) {

			long initial = 100 * 1000;
			long adder = -i * 1000; // Reverse order
			int output = i;
			/*
			 * long initial = i*1000; long adder = 1; //Reverse order int output
			 * = i%parallelism+1;
			 */

			Tuple2 t = Tuple2.of(i % parallelism, output);
			System.err.println(t);
			Event e = new Event(initial + adder, t);
			data.get(i % parallelism).add(e);
		}

		StreamExecutionEnvironment execEnv = StreamExecutionEnvironment.createLocalEnvironment(parallelism);

		execEnv.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);

		// Slightly out of order
		DataStream<Event> eventStream = execEnv.addSource(new SampleRichParallelSourceNoTSOrWM(data, delay))
				.assignTimestampsAndWatermarks(new MyBoundedOutOfOrderedness(Time.seconds(10)));// Try
																								// with
																								// 5

		DataStream<Tuple2<Integer, Integer>> selectDS = eventStream.map(new SimpleMapper());

		int windowInterval = 20;
		System.out.println("Window Interval " + windowInterval);
		selectDS.keyBy(0).timeWindow(Time.seconds(windowInterval)).sum(1).printToErr();// This
																						// output
																						// is
																						// very
																						// interesting

		execEnv.execute("Sample Event Time Pipeline Out of order with Periodic WM");
	}

	public void executeJobOutOfOrderWithPeriodicWM(int parallelism, int noOfEvents, long standardDelayInSeconds,
			long anamolousDelayInSeconds, int watermarkEveryNInterval) throws Exception {
		Map<Integer, List<Event>> data = new HashMap<>();
		Map<Integer, Long> delay = new HashMap<>();
		for (int i = 0; i < (parallelism - 1); i++) {
			delay.put(i, standardDelayInSeconds);
		}
		delay.put((parallelism - 1), anamolousDelayInSeconds);
		for (int i = 0; i < parallelism; i++) {
			data.put(i, new ArrayList<Event>());
		}
		for (int i = 0; i < noOfEvents; i++) {

			long initial = 100 * 1000;
			long adder = -i * 1000; // Reverse order
			int output = i;
			/*
			 * long initial = i*1000; long adder = 1; //Reverse order int output
			 * = i%parallelism+1;
			 */

			Tuple2 t = Tuple2.of(i % parallelism, output);
			System.err.println(t);
			Event e = new Event(initial + adder, t);
			data.get(i % parallelism).add(e);
		}

		StreamExecutionEnvironment execEnv = StreamExecutionEnvironment.createLocalEnvironment(parallelism);

		execEnv.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);

		// Slightly out of order
		DataStream<Event> eventStream = execEnv.addSource(new SampleRichParallelSourceNoTSOrWM(data, delay))
				.assignTimestampsAndWatermarks(new WaterMarkAssigner());

		DataStream<Tuple2<Integer, Integer>> selectDS = eventStream.map(new SimpleMapper());

		int windowInterval = 20;
		System.out.println("Window Interval " + windowInterval);
		selectDS.keyBy(0).timeWindow(Time.seconds(windowInterval)).allowedLateness(Time.seconds(1000)).apply(new MyApplyFunction()).printToErr();
		execEnv.execute("Sample Event Time Pipeline Out of order with Periodic WM");
	}

	public void executeJobOutOfOrder(int parallelism, int noOfEvents, long standardDelayInSeconds,
			long anamolousDelayInSeconds, int watermarkEveryNInterval) throws Exception {
		Map<Integer, List<Event>> data = new HashMap<>();
		Map<Integer, Long> delay = new HashMap<>();
		for (int i = 0; i < (parallelism - 1); i++) {
			delay.put(i, standardDelayInSeconds);
		}
		delay.put((parallelism - 1), anamolousDelayInSeconds);
		for (int i = 0; i < parallelism; i++) {
			data.put(i, new ArrayList<Event>());
		}
		for (int i = 0; i < noOfEvents; i++) {

			long initial = 100 * 1000;
			long adder = -i * 1000; // Reverse order
			int output = i;
			/*
			 * long initial = i*1000; long adder = 1; //Reverse order int output
			 * = i%parallelism+1;
			 */

			Tuple2 t = Tuple2.of(i % parallelism, output);
			System.err.println(t);
			Event e = new Event(initial + adder, t);
			data.get(i % parallelism).add(e);
		}

		StreamExecutionEnvironment execEnv = StreamExecutionEnvironment.createLocalEnvironment(parallelism);

		execEnv.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);

		// Slightly out of order
		DataStream<Event> eventStream = execEnv.addSource(new SampleRichParallelSourceNoTSOrWM(data, delay))
				.assignTimestampsAndWatermarks(new MyAscendingTimestampExtractor());

		DataStream<Tuple2<Integer, Integer>> selectDS = eventStream.map(new SimpleMapper());

		int windowInterval = 20;
		System.out.println("Window Interval " + windowInterval);

		selectDS.keyBy(0).timeWindow(Time.seconds(windowInterval)).allowedLateness(Time.seconds(1000)).apply(new MyApplyFunction()).printToErr();
				

		/*
		selectDS.keyBy(0).timeWindow(Time.seconds(windowInterval)).apply(new MyApplyFunction())
																  .printToErr();//Only the first element is printed
*/
		execEnv.execute("Sample Event Time Pipeline");
	}

	public void executeJob(int parallelism, int noOfEvents, long standardDelayInSeconds, long anamolousDelayInSeconds,
			int watermarkEveryNInterval) throws Exception {
		Map<Integer, List<Event>> data = new HashMap<>();
		Map<Integer, Long> delay = new HashMap<>();
		for (int i = 0; i < (parallelism - 1); i++) {
			delay.put(i, standardDelayInSeconds);
		}
		delay.put((parallelism - 1), anamolousDelayInSeconds);
		for (int i = 0; i < parallelism; i++) {
			data.put(i, new ArrayList<Event>());
		}
		for (int i = 0; i < noOfEvents; i++) {
			// long adder = -i*20000;
			long adder = 1;
			Event e = new Event(i * 1000 + adder, Tuple2.of(i % parallelism, i % parallelism + 1));
			data.get(i % parallelism).add(e);
		}

		StreamExecutionEnvironment execEnv = StreamExecutionEnvironment.createLocalEnvironment(parallelism);

		execEnv.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);

		// DataStream<Event> eventStream = execEnv.addSource(new
		// SampleRichParallelSource(data,delay,watermarkEveryNInterval));
		// DataStream<Event> eventStream = execEnv.addSource(new
		// SampleRichParallelSourceNoTSOrWM(data,delay)).assignTimestampsAndWatermarks(new
		// MyAscendingTimestampExtractor());

		// Slightly out of order
		DataStream<Event> eventStream = execEnv.addSource(new SampleRichParallelSourceNoTSOrWM(data, delay))
				.assignTimestampsAndWatermarks(new MyAscendingTimestampExtractor());

		DataStream<Tuple2<Integer, Integer>> selectDS = eventStream.map(new SimpleMapper());

		int windowInterval = 20;
		System.out.println("Window Interval " + windowInterval);
		selectDS.keyBy(0).timeWindow(Time.seconds(windowInterval)).sum(1).printToErr();

		execEnv.execute("Sample Event Time Pipeline");
	}

	public static void main(String[] args) throws Exception {
		SampleParallelSourceStreamingPipeline window = new SampleParallelSourceStreamingPipeline();
		 //window.executeJobOutOfOrder(1, 41, 1l, 1l, 2);
		 window.executeJobOutOfOrderWithPeriodicWM(1, 41, 1l, 1l, 2);
		//window.executeJobOutOfOrderWithBoundedLateness(1, 40, 1l, 1l, 2);

	}

	public static class SimpleMapper implements MapFunction<Event, Tuple2<Integer, Integer>> {
		@Override
		public Tuple2<Integer, Integer> map(Event value) throws Exception {
			// TODO Auto-generated method stub
			return value.getData();
		}
	}

	public static class MyAscendingTimestampExtractor extends AscendingTimestampExtractor<Event> {

		@Override
		public long extractAscendingTimestamp(Event element) {
			// TODO Auto-generated method stub
			return element.getTimestamp();
		}
	}

	private static class WaterMarkAssigner implements AssignerWithPeriodicWatermarks<Event> {
		private static final long serialVersionUID = 1L;
		private long maxTimestamp = 0;
		private long priorTimestamp = 0;

		@Override
		public Watermark getCurrentWatermark() {
			if (maxTimestamp > 0 && maxTimestamp == priorTimestamp) {
				long advance = 200;
				maxTimestamp += advance;// Start advancing
			}
			priorTimestamp = maxTimestamp;
			// Date w = new Date(maxTimestamp);
			if (maxTimestamp > 0) {
				String TS_PATTERN = "MM/dd/yyyy hh:mm:ss a";
				// System.err.println(DateTimeFormat.forPattern(TS_PATTERN).print(maxTimestamp));
				return new Watermark(this.maxTimestamp);
			} else {
				return null;
			}

		}

		@Override
		public long extractTimestamp(Event element, long previousElementTimestamp) {
			long millis = element.getTimestamp();
			maxTimestamp = Math.max(maxTimestamp, millis);
			// Date m = new Date(millis);

			return Long.valueOf(millis);
		}
	}

	/**
	 * This generator generates watermarks assuming that elements come out of
	 * order to a certain degree only. The latest elements for a certain
	 * timestamp t will arrive at most n milliseconds after the earliest
	 * elements for timestamp t.
	 */
	private static class MyBoundedOutOfOrderedness extends BoundedOutOfOrdernessTimestampExtractor<Event> {

		public MyBoundedOutOfOrderedness(Time maxOutOfOrderness) {
			super(maxOutOfOrderness);
		}

		@Override
		public long extractTimestamp(Event element) {
			return element.getTimestamp();
		}

	}

	/**
	 * This generator generates watermarks that are lagging behind processing
	 * time by a certain amount. It assumes that elements arrive in Flink after
	 * at most a certain time.
	 */
	public class TimeLagWatermarkGenerator implements AssignerWithPeriodicWatermarks<Event> {

		private final long maxTimeLag = 5000; // 5 seconds

		@Override
		public long extractTimestamp(Event element, long previousElementTimestamp) {
			return element.getTimestamp();
		}

		@Override
		public Watermark getCurrentWatermark() {
			// return the watermark as current time minus the maximum time lag
			return new Watermark(System.currentTimeMillis() - maxTimeLag);
		}
	}

	@SuppressWarnings("serial")
	public class MyApplyFunction implements
			WindowFunction<Tuple2<Integer, Integer>, Tuple4<Long, Long, List<Integer>, Integer>, Tuple, TimeWindow> {
		@Override
		public void apply(Tuple key, TimeWindow window, Iterable<Tuple2<Integer, Integer>> inputs,
				Collector<Tuple4<Long, Long, List<Integer>, Integer>> out) throws Exception {

			List<Integer> eventIds = new ArrayList<>(0);
			int sum = 0;
			Iterator<Tuple2<Integer, Integer>> iter = inputs.iterator();
			while (iter.hasNext()) {
				Tuple2<Integer, Integer> input = iter.next();
				eventIds.add(input.f1);
				sum += input.f1;
			}

			out.collect(new Tuple4<>(window.getStart(), window.getEnd(), eventIds, sum));

		}
	}

}
