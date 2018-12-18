package kr.ac.konkuk.ccslab.cm.info;
import java.util.*;
import java.util.concurrent.ExecutorService;

import kr.ac.konkuk.ccslab.cm.entity.CMSendVideoInfo;
import kr.ac.konkuk.ccslab.cm.entity.CMList;
import kr.ac.konkuk.ccslab.cm.entity.CMRecvVideoInfo;

import java.io.*;

public class CMStreamingInfo {
	private String m_strVideoPath;
	private Hashtable<String, CMList<CMSendVideoInfo>> m_sendVideoHashtable; // key is the receiver name
	private Hashtable<String, CMList<CMRecvVideoInfo>> m_recvVideoHashtable; // key is the sender name
	private ExecutorService m_executorService;
	
	public CMStreamingInfo()
	{
		m_strVideoPath = null;
		m_sendVideoHashtable = new Hashtable<String, CMList<CMSendVideoInfo>>();
		m_recvVideoHashtable = new Hashtable<String, CMList<CMRecvVideoInfo>>();
		m_executorService = null;
	}
	
	////////// set/get methods
	
	public void setVideoPath(String path)
	{
		m_strVideoPath = path;
	}
	
	public String getVideoPath()
	{
		return m_strVideoPath;
	}
	
	public void setExecutorService(ExecutorService es)
	{
		m_executorService = es;
	}
	
	public ExecutorService getExecutorService()
	{
		return m_executorService;
	}
	
	////////// add/remove/find sending Video info
	
	public boolean addSendVideoInfo(String uName, String fPath, long lSize, int nContentID)
	{
		CMSendVideoInfo sInfo = null;
		String strVideoName = null;
		CMList<CMSendVideoInfo> sInfoList = null;
		boolean bResult = false;
		
		strVideoName = fPath.substring(fPath.lastIndexOf(File.separator)+1);
		sInfo = new CMSendVideoInfo();
		sInfo.setReceiverName(uName);
		sInfo.setVideoName(strVideoName);
		sInfo.setVideoPath(fPath);
		sInfo.setVideoSize(lSize);
		sInfo.setContentID(nContentID);
		
		sInfoList = m_sendVideoHashtable.get(uName);
		if(sInfoList == null)
		{
			sInfoList = new CMList<CMSendVideoInfo>();
			m_sendVideoHashtable.put(uName, sInfoList);
		}
		
		bResult = sInfoList.addElement(sInfo);
		if(!bResult)
		{
			System.err.println("CMVideoTransferInfo.addSendVideoInfo() failed: "+sInfo.toString());
			return false;
		}
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMVideoTransferInfo.addSendVideoInfo() done: "+sInfo.toString());
			System.out.println("# current hashtable elements: "+m_sendVideoHashtable.size());
		}

		return true;
	}

	public boolean addSendVideoInfo(CMSendVideoInfo sInfo)
	{
		String strVideoName = null;
		CMList<CMSendVideoInfo> sInfoList = null;
		boolean bResult = false;
		
		strVideoName = sInfo.getVideoPath().substring(sInfo.getVideoPath().lastIndexOf(File.separator)+1);
		sInfo.setVideoName(strVideoName);
		
		sInfoList = m_sendVideoHashtable.get(sInfo.getReceiverName());
		if(sInfoList == null)
		{
			sInfoList = new CMList<CMSendVideoInfo>();
			m_sendVideoHashtable.put(sInfo.getReceiverName(), sInfoList);
		}
		
		bResult = sInfoList.addElement(sInfo);
		if(!bResult)
		{
			System.err.println("CMVideoTransferInfo.addSendVideoInfo() failed: "+sInfo.toString());
			return false;
		}
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMVideoTransferInfo.addSendVideoInfo() done: "+sInfo.toString());
			System.out.println("# current hashtable elements: "+m_sendVideoHashtable.size());
		}

		return true;		
	}

	public CMSendVideoInfo findSendVideoInfo(String uName, String fName, int nContentID)
	{
		CMSendVideoInfo sInfo = null;
		CMList<CMSendVideoInfo> sInfoList = null;
		CMSendVideoInfo tInfo = null;
		
		sInfoList = m_sendVideoHashtable.get(uName);
		if(sInfoList == null)
		{
			System.err.println("CMVideoTransferInfo.findSendVideoInfo(), list not found for receiver("
					+uName+")");
			return null;
		}
		
		tInfo = new CMSendVideoInfo();
		tInfo.setReceiverName(uName);
		tInfo.setVideoName(fName);
		tInfo.setContentID(nContentID);
		
		sInfo = sInfoList.findElement(tInfo);
		
		if(sInfo == null)
		{
			System.err.println("CMVideoTransferInfo.findSendVideoInfo(), not found!: "+tInfo.toString());
			return null;
		}
		
		return sInfo;
	}

	public boolean removeSendVideoInfo(String uName, String fName, int nContentID)
	{
		CMList<CMSendVideoInfo> sInfoList = null;
		CMSendVideoInfo sInfo = null;
		boolean bResult = false;

		sInfoList = m_sendVideoHashtable.get(uName);
		if(sInfoList == null)
		{
			System.err.println("CMVideoTransferInfo.removeSendVideoInfo(), list not found for receiver("
					+uName+")");
			return false;
		}
		
		sInfo = new CMSendVideoInfo();
		sInfo.setReceiverName(uName);
		sInfo.setVideoName(fName);
		sInfo.setContentID(nContentID);
		bResult = sInfoList.removeElement(sInfo);

		if(!bResult)
		{
			System.err.println("CMVideoTransferInfo.removeSendVideoInfo() error! : "+sInfo.toString());
			return false;
		}
		
		if(sInfoList.isEmpty())
		{
			m_sendVideoHashtable.remove(uName);
		}

		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMVideoTransferInfo.removeSendVideoInfo() done : "+sInfo.toString());
			System.out.println("# current hashtable elements: "+m_sendVideoHashtable.size());
		}
		
		return true;
		
	}
	
	public Hashtable<String, CMList<CMSendVideoInfo>> getSendVideoHashtable()
	{
		return m_sendVideoHashtable;
	}

	////////// add/remove/find receiving Video info

	public boolean addRecvVideoInfo(String senderName, String fName, long lSize, int nContentID,
			long lRecvSize, RandomAccessFile writeVideo)
	{
		CMRecvVideoInfo rInfo = null;
		CMList<CMRecvVideoInfo> rInfoList = null;
		boolean bResult = false;

		rInfo = null;
		rInfo = new CMRecvVideoInfo();
		rInfo.setSenderName(senderName);
		rInfo.setVideoName(fName);
		rInfo.setVideoSize(lSize);
		rInfo.setContentID(nContentID);
		rInfo.setRecvSize(lRecvSize);
		rInfo.setWriteVideo(writeVideo);
		
		rInfoList = m_recvVideoHashtable.get(senderName);
		if(rInfoList == null)
		{
			rInfoList = new CMList<CMRecvVideoInfo>();
			m_recvVideoHashtable.put(senderName, rInfoList);
		}
		
		bResult = rInfoList.addElement(rInfo);
		if(!bResult)
		{
			System.err.println("CMVideoTransferInfo.addRecvVideoInfo() failed: "+rInfo.toString());
			return false;
		}
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMVideoTransferInfo.addRecvVideoInfo() done: "+rInfo.toString());
			System.out.println("# current hashtable elements: "+m_recvVideoHashtable.size());
		}
		
		return true;
	}
	
	public boolean addRecvVideoInfo(CMRecvVideoInfo rInfo)
	{
		CMList<CMRecvVideoInfo> rInfoList = null;
		boolean bResult = false;
		
		rInfoList = m_recvVideoHashtable.get(rInfo.getSenderName());
		if(rInfoList == null)
		{
			rInfoList = new CMList<CMRecvVideoInfo>();
			m_recvVideoHashtable.put(rInfo.getSenderName(), rInfoList);
		}
		
		bResult = rInfoList.addElement(rInfo);
		if(!bResult)
		{
			System.err.println("CMVideoTransferInfo.addRecvVideoInfo() failed: "+rInfo.toString());
			return false;
		}
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMVideoTransferInfo.addRecvVideoInfo() done: "+rInfo.toString());
			System.out.println("# current hashtable elements: "+m_recvVideoHashtable.size());
		}
		
		return true;		
	}

	public CMRecvVideoInfo findRecvVideoInfo(String senderName, String fName, int nContentID)
	{	
		CMRecvVideoInfo rInfo = null;
		CMList<CMRecvVideoInfo> rInfoList = null;
		CMRecvVideoInfo tInfo = null;
		
		rInfoList = m_recvVideoHashtable.get(senderName);
		if(rInfoList == null)
		{
			System.err.println("CMVideoTransferInfo.findRecvVideoInfo(), list not found for sender("
					+senderName+")");
			return null;
		}
		
		tInfo = new CMRecvVideoInfo();
		tInfo.setSenderName(senderName);
		tInfo.setVideoName(fName);
		tInfo.setContentID(nContentID);
		
		rInfo = rInfoList.findElement(tInfo);
		
		if(rInfo == null)
		{
			System.err.println("CMVideoTransferInfo.findRecvVideoInfo(), not found!: "+tInfo.toString());
			return null;
		}
				
		return rInfo;

	}

	public boolean removeRecvVideoInfo(String senderName, String fName, int nContentID)
	{
		CMList<CMRecvVideoInfo> rInfoList = null;
		CMRecvVideoInfo rInfo = null;
		boolean bResult = false;

		rInfoList = m_recvVideoHashtable.get(senderName);
		if(rInfoList == null)
		{
			System.err.println("CMVideoTransferInfo.removeRecvVideoInfo(), list not found for sender("
					+senderName+")");
			return false;
		}
		
		rInfo = new CMRecvVideoInfo();
		rInfo.setSenderName(senderName);
		rInfo.setVideoName(fName);
		rInfo.setContentID(nContentID);
		bResult = rInfoList.removeElement(rInfo);

		if(!bResult)
		{
			System.err.println("CMVideoTransferInfo.removeRecvVideoInfo() error! : "+rInfo.toString());
			return false;
		}
		
		if(rInfoList.isEmpty())
		{
			m_sendVideoHashtable.remove(senderName);
		}

		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMVideoTransferInfo.removeSendVideoInfo() done : "+rInfo.toString());
			System.out.println("# current hashtable elements: "+m_sendVideoHashtable.size());
		}
		
		return true;
		
	}
	
	public Hashtable<String, CMList<CMRecvVideoInfo>> getRecvVideoHashtable()
	{
		return m_recvVideoHashtable;
	}
	
	////// find the receiving Video info that is being used or is not yet started by the thread pool
	
	public CMRecvVideoInfo findRecvVideoInfoNotStarted(String strSender)
	{
		CMRecvVideoInfo rfInfo = null;
		CMList<CMRecvVideoInfo> rfInfoList = m_recvVideoHashtable.get(strSender);
		boolean bFound = false;
		
		if(rfInfoList == null) return null;
		
		Iterator<CMRecvVideoInfo> iter = rfInfoList.getList().iterator();
		while(iter.hasNext() && !bFound)
		{
			rfInfo = iter.next();
			if(rfInfo.getRecvTaskResult() == null)
			{
				bFound = true;
				if(CMInfo._CM_DEBUG)
					System.out.println("CMVideoTransferInfo.findRecvVideoInfoNotStarted(); found: "+rfInfo.toString());
			}
		}
		
		if(bFound)
			return rfInfo;
		else
		{
			if(CMInfo._CM_DEBUG)
				System.out.println("CMVideoTransferInfo.findRecvVideoInfoNotStarted(); not found!");
			return null;
		}
	}
	
	public boolean isRecvOngoing(String strSender)
	{
		CMRecvVideoInfo rfInfo = null;
		CMList<CMRecvVideoInfo> rfInfoList = m_recvVideoHashtable.get(strSender);
		boolean bFound = false;
		
		if(rfInfoList == null) return false;
		
		Iterator<CMRecvVideoInfo> iter = rfInfoList.getList().iterator();
		while(iter.hasNext() && !bFound)
		{
			rfInfo = iter.next();
			if(rfInfo.getRecvTaskResult() != null)
			{
				bFound = true;
				if(CMInfo._CM_DEBUG)
					System.out.println("CMVideoTransferInfo.isRecvOngoing(); ongoing recv info found: "+rfInfo.toString());
			}
		}
		
		return bFound;
	}
	
}
