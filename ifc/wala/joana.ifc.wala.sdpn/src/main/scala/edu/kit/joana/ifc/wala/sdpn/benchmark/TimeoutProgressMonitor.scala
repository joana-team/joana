package edu.kit.joana.ifc.wala.sdpn.benchmark

import com.ibm.wala.util.MonitorUtil.IProgressMonitor;

class TimeoutProgressMonitor(timeout: Long, delegate: IProgressMonitor) extends IProgressMonitor {
    private val end = if(timeout > 0) System.currentTimeMillis + timeout else Long.MaxValue
    private var canceled = false

    def beginTask(name: String, totalWork: Int): Unit = { if (delegate != null) delegate.beginTask(name, totalWork) }

    def done(): Unit = { if (delegate != null) delegate.done() }

    def isCanceled(): Boolean = { System.currentTimeMillis > end || (delegate != null && delegate.isCanceled) || canceled }

    def cancel(): Unit = { if (delegate != null) delegate.cancel() else canceled = true }

    def subTask(name: String): Unit = { if (delegate != null) delegate.subTask(name) }

    def worked(work: Int): Unit = { if (delegate != null) delegate.worked(work) }

}