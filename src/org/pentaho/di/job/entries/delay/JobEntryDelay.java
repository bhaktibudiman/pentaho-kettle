/**********************************************************************
 **                                                                   **
 **               This code belongs to the KETTLE project.            **
 **                                                                   **
 ** Kettle, from version 2.2 on, is released into the public domain   **
 ** under the Lesser GNU Public License (LGPL).                       **
 **                                                                   **
 ** For more details, please read the document LICENSE.txt, included  **
 ** in this project                                                   **
 **                                                                   **
 ** http://www.kettle.be                                              **
 ** info@kettle.be                                                    **
 **                                                                   **
 **********************************************************************/

package org.pentaho.di.job.entries.delay;

import java.util.List;

import org.eclipse.swt.widgets.Shell;
import org.pentaho.di.core.CheckResult;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.Result;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleDatabaseException;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.logging.LogWriter;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.job.Job;
import org.pentaho.di.job.JobEntryType;
import org.pentaho.di.job.JobMeta;
import org.pentaho.di.job.entry.JobEntryBase;
import org.pentaho.di.job.entry.JobEntryDialogInterface;
import org.pentaho.di.job.entry.JobEntryInterface;
import org.pentaho.di.repository.Repository;
import org.w3c.dom.Node;

/**
 * Job entry type to sleep for a time. It uses a piece of javascript to do this.
 *
 * @author Samatar
 * @since 21-02-2007
 */
public class JobEntryDelay extends JobEntryBase implements Cloneable, JobEntryInterface {
  static private String DEFAULT_MAXIMUM_TIMEOUT = "0"; //$NON-NLS-1$

  private String maximumTimeout; // maximum timeout in seconds

  public int scaleTime;

  public JobEntryDelay(String n) {
    super(n, ""); //$NON-NLS-1$
    setID(-1L);
    setJobEntryType(JobEntryType.DELAY);
  }

  public JobEntryDelay() {
    this(""); //$NON-NLS-1$
  }

  public JobEntryDelay(JobEntryBase jeb) {
    super(jeb);
  }

  public Object clone() {
    JobEntryDelay je = (JobEntryDelay) super.clone();
    return je;
  }

  public String getXML() {
    StringBuffer retval = new StringBuffer(200);

    retval.append(super.getXML());
    retval.append("      ").append(XMLHandler.addTagValue("maximumTimeout", maximumTimeout)); //$NON-NLS-1$//$NON-NLS-2$
    retval.append("      ").append(XMLHandler.addTagValue("scaletime", scaleTime)); //$NON-NLS-1$ //$NON-NLS-2$

    return retval.toString();
  }

  public void loadXML(Node entrynode, List<DatabaseMeta> databases, Repository rep) throws KettleXMLException {
    try {
      super.loadXML(entrynode, databases);
      maximumTimeout = XMLHandler.getTagValue(entrynode, "maximumTimeout"); //$NON-NLS-1$
      scaleTime = Integer.parseInt(XMLHandler.getTagValue(entrynode, "scaletime")); //$NON-NLS-1$
    } catch (Exception e) {
      throw new KettleXMLException(Messages.getString("JobEntryDelay.UnableToLoadFromXml.Label"), e); //$NON-NLS-1$
    }
  }

  public void loadRep(Repository rep, long id_jobentry, List<DatabaseMeta> databases) throws KettleException {
    try {
      super.loadRep(rep, id_jobentry, databases);

      maximumTimeout = rep.getJobEntryAttributeString(id_jobentry, "maximumTimeout"); //$NON-NLS-1$
      scaleTime = (int) rep.getJobEntryAttributeInteger(id_jobentry, "scaletime"); //$NON-NLS-1$
    } catch (KettleDatabaseException dbe) {
      throw new KettleException(Messages.getString("JobEntryDelay.UnableToLoadFromRepo.Label") //$NON-NLS-1$
          + id_jobentry, dbe);
    }
  }

  //
  // Save the attributes of this job entry
  //
  public void saveRep(Repository rep, long id_job) throws KettleException {
    try {
      super.saveRep(rep, id_job);

      rep.saveJobEntryAttribute(id_job, getID(), "maximumTimeout", maximumTimeout); //$NON-NLS-1$
      rep.saveJobEntryAttribute(id_job, getID(), "scaletime", scaleTime); //$NON-NLS-1$
    } catch (KettleDatabaseException dbe) {
      throw new KettleException(Messages.getString("JobEntryDelay.UnableToSaveToRepo.Label") + id_job, dbe); //$NON-NLS-1$
    }
  }

  /**
   * Execute this job entry and return the result.
   * In this case it means, just set the result boolean in the Result class.
   * @param previousResult The result of the previous execution
   * @return The Result of the execution.
   */
  public Result execute(Result previousResult, int nr, Repository rep, Job parentJob) {
    LogWriter log = LogWriter.getInstance();
    Result result = previousResult;
    result.setResult(false);
    int Multiple;
    String Waitscale;

    // Scale time
    if (scaleTime == 0) {
      // Second
      Multiple = 1000;
      Waitscale = Messages.getString("JobEntryDelay.SScaleTime.Label"); //$NON-NLS-1$

    } else if (scaleTime == 1) {
      // Minute
      Multiple = 60000;
      Waitscale = Messages.getString("JobEntryDelay.MnScaleTime.Label"); //$NON-NLS-1$
    } else {
      // Hour
      Multiple = 3600000;
      Waitscale = Messages.getString("JobEntryDelay.HrScaleTime.Label"); //$NON-NLS-1$
    }
    try {
      // starttime (in seconds ,Minutes or Hours)
      long timeStart = System.currentTimeMillis() / Multiple;

      long iMaximumTimeout = Const.toInt(getMaximumTimeout(), Const.toInt(DEFAULT_MAXIMUM_TIMEOUT, 0));

      if (log.isDetailed()) {
        log.logDetailed(toString(), Messages.getString("JobEntryDelay.LetsWaitFor.Label", String //$NON-NLS-1$
            .valueOf(iMaximumTimeout), String.valueOf(Waitscale)));
      }

      boolean continueLoop = true;
      //
      // Sanity check on some values, and complain on insanity
      //
      if (iMaximumTimeout < 0) {
        iMaximumTimeout = Const.toInt(DEFAULT_MAXIMUM_TIMEOUT, 0);
        log.logBasic(toString(), Messages.getString(
            "JobEntryDelay.MaximumTimeReset.Label", String.valueOf(iMaximumTimeout), String.valueOf(Waitscale))); //$NON-NLS-1$
      }

      // TODO don't contineously loop, but sleeping would be better.
      while (continueLoop && !parentJob.isStopped()) {
        // Update Time value
        long now = System.currentTimeMillis() / Multiple;

        // Let's check the limit time
        if ((iMaximumTimeout > 0) && (now >= (timeStart + iMaximumTimeout))) {
          // We have reached the time limit
          if (log.isDetailed()) {
            log.logDetailed(toString(), Messages.getString("JobEntryDelay.WaitTimeIsElapsed.Label")); //$NON-NLS-1$
          }
          continueLoop = false;
          result.setResult(true);
        }
      }
    } catch (Exception e) {
      // We get an exception
      result.setResult(false);
      log.logError(toString(), "Error  : " + e.getMessage()); //$NON-NLS-1$
    }

    return result;
  }

  public boolean resetErrorsBeforeExecution() {
    // we should be able to evaluate the errors in
    // the previous jobentry.
    return false;
  }

  public boolean evaluates() {
    return true;
  }

  public boolean isUnconditional() {
    return false;
  }

  public JobEntryDialogInterface getDialog(Shell shell, JobEntryInterface jei, JobMeta jobMeta, String jobName,
      Repository rep) {
    return new JobEntryDelayDialog(shell, this, jobMeta);
  }

  public String getMaximumTimeout() {
    return environmentSubstitute(maximumTimeout);
  }

  public void setMaximumTimeout(String s) {
    maximumTimeout = s;
  }

  public void check(List<CheckResult> remarks, JobMeta jobMeta) {
    remarks.add(new CheckResult(CheckResult.TYPE_RESULT_OK, Messages.getString(
        "JobEntryDelay.CheckRemark.MaximumTimeout", maximumTimeout), this)); //$NON-NLS-1$
    remarks.add(new CheckResult(CheckResult.TYPE_RESULT_OK, Messages.getString("JobEntryDelay.CheckRemark.ScaleTime", //$NON-NLS-1$
        String.valueOf(scaleTime)), this));

  }
}