package org.hadoop.test.jobs.tasks.utils.directed;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.mapred.jobcontrol.Job;
import org.apache.hadoop.util.Tool;
import org.hadoop.test.data.directed.DirectedNodeNeighbourhood;
import org.hadoop.test.map.directed.GatherDirectedNodeNeighboursInfoMap;
import org.hadoop.test.reduce.directed.GatherDirectedNodeNeighboursInfoReducer;

import java.io.IOException;

public class GatherDirectedNodeNeighboursInfoJob extends Configured implements Tool {
    public int run(String[] args) throws IOException {

        long t0 = System.currentTimeMillis();

        JobConf gatherNodeNeighboursInfoConf = new JobConf(new Configuration());
        Job job2 = new Job(gatherNodeNeighboursInfoConf);
        gatherNodeNeighboursInfoConf.setJarByClass(GatherDirectedNodeNeighboursInfoJob.class);

        gatherNodeNeighboursInfoConf.setMapOutputKeyClass(Text.class);
        gatherNodeNeighboursInfoConf.setMapOutputValueClass(Text.class);

        gatherNodeNeighboursInfoConf.setMapperClass(GatherDirectedNodeNeighboursInfoMap.class);
        gatherNodeNeighboursInfoConf.setReducerClass(GatherDirectedNodeNeighboursInfoReducer.class);

        gatherNodeNeighboursInfoConf.setOutputKeyClass(NullWritable.class);
        gatherNodeNeighboursInfoConf.setOutputValueClass(DirectedNodeNeighbourhood.class);

        gatherNodeNeighboursInfoConf.setInputFormat(TextInputFormat.class);
        gatherNodeNeighboursInfoConf.setOutputFormat(TextOutputFormat.class);

        FileInputFormat.addInputPath(gatherNodeNeighboursInfoConf, new Path(args[5] + "/nodeGraph"));
        FileOutputFormat.setOutputPath(gatherNodeNeighboursInfoConf, new Path(args[5] + "/nodeNeighbourhood"));

        gatherNodeNeighboursInfoConf.setNumMapTasks(Integer.parseInt(args[2]));
        gatherNodeNeighboursInfoConf.setNumReduceTasks(Integer.parseInt(args[3]));

        //platform config
        /* Comment to to perform test in local-mode

        /* pseudo */
        //gatherNodeNeighboursInfoConf.set("io.sort.mb", "768");
        //gatherNodeNeighboursInfoConf.set("fs.inmemory.size.mb", "768");
        //gatherNodeNeighboursInfoConf.set("io.sort.factor", "50");

        /* DAS4 conf Hadoop ver 0.20.203 */
        gatherNodeNeighboursInfoConf.set("io.sort.mb", "1536");
        gatherNodeNeighboursInfoConf.set("io.sort.factor", "80");
        gatherNodeNeighboursInfoConf.set("fs.inmemory.size.mb", "1536");

        JobClient.runJob(gatherNodeNeighboursInfoConf);

        long t1 = System.currentTimeMillis();
        double elapsedTimeSeconds = (t1 - t0)/1000.0;
        System.out.println("NodeNeigh_Texe = "+elapsedTimeSeconds);

        if(Boolean.parseBoolean(args[1])) {
            System.out.println("\n@@@ Deleting intermediate results");
            FileSystem dfs = FileSystem.get(gatherNodeNeighboursInfoConf);
            dfs.delete(new Path(args[5]+"/nodeGraph"), true);
        }

        System.out.println("\n*****************************************");
        System.out.println("* node neighbourhood retrieved FINISHED *");
        System.out.println("*****************************************\n");

        return 0;
    }
}