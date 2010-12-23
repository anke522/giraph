package com.yahoo.hadoop_bsp;

import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;
import org.apache.log4j.Logger;

/**
 * Master thread that will coordinate the activities of the tasks.  It runs
 * on all task processes, however, will only execute its algorithm if it knows
 * it is the "leader" from ZooKeeper.
 * @author aching
 *
 */
public class MasterThread<
    I extends WritableComparable, V, E, M extends Writable> extends Thread {
    /** Class logger */
    private static final Logger LOG = Logger.getLogger(MasterThread.class);
    /** Reference to shared BspService */
    private CentralizedServiceMaster<I, V, E, M> m_bspServiceMaster = null;

    /** Constructor */
    MasterThread(BspServiceMaster<I, V, E, M> bspServiceMaster) {
        m_bspServiceMaster = bspServiceMaster;
    }

    /**
     * The master algorithm.  The algorithm should be able to withstand
     * failures and resume as necessary since the master may switch during a
     * job.
     */
    @Override
    public void run() {
        try {
            m_bspServiceMaster.setup();
            if (m_bspServiceMaster.becomeMaster() == true) {
                m_bspServiceMaster.createInputSplits();
                while (m_bspServiceMaster.coordinateSuperstep() != true) {
                    LOG.info("masterThread: Finished superstep " +
                             (m_bspServiceMaster.getSuperstep() - 1));
                }
                m_bspServiceMaster.setJobState(BspService.State.FINISHED);
            }
            m_bspServiceMaster.cleanup();
        } catch (Exception e) {
            LOG.error("masterThread: Master algorithm failed: " +
                      e.getMessage());
            throw new RuntimeException(e);
        }
    }
}