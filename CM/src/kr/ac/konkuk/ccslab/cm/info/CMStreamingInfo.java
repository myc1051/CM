package kr.ac.konkuk.ccslab.cm.info;
import java.util.*;
import java.util.concurrent.ExecutorService;

import kr.ac.konkuk.ccslab.cm.entity.CMSendFileInfo;
import kr.ac.konkuk.ccslab.cm.entity.CMList;
import kr.ac.konkuk.ccslab.cm.entity.CMRecvFileInfo;

import java.io.*;

public class CMStreamingInfo {
	private String m_strFilePath;
	private Hashtable<String, CMList<CMSendFileInfo>> m_sendFileHashtable; // key is the receiver name
	private Hashtable<String, CMList<CMRecvFileInfo>> m_recvFileHashtable; // key is the sender name
	private ExecutorService m_executorService;
	
	public CMStreamingInfo()
	{
		m_strFilePath = null;
		m_sendFileHashtable = new Hashtable<String, CMList<CMSendFileInfo>>();
		m_recvFileHashtable = new Hashtable<String, CMList<CMRecvFileInfo>>();
		m_executorService = null;
	}
	
	////////// set/get methods
	
	public void setFilePath(String path)
	{
		m_strFilePath = path;
	}
	
	public String getFilePath()
	{
		return m_strFilePath;
	}
	
	public void setExecutorService(ExecutorService es)
	{
		m_executorService = es;
	}
	
	public ExecutorService getExecutorService()
	{
		return m_executorService;
	}
	
	////////// add/remove/find sending file info
	
	public boolean addSendFileInfo(String uName, String fPath, long lSize, int nContentID)
	{
		CMSendFileInfo sInfo = null;
		String strFileName = null;
		CMList<CMSendFileInfo> sInfoList = null;
		boolean bResult = false;
		
		strFileName = fPath.substring(fPath.lastIndexOf(File.separator)+1);
		sInfo = new CMSendFileInfo();
		sInfo.setReceiverName(uName);
		sInfo.setFileName(strFileName);
		sInfo.setFilePath(fPath);
		sInfo.setFileSize(lSize);
		sInfo.setContentID(nContentID);
		
		sInfoList = m_sendFileHashtable.get(uName);
		if(sInfoList == null)
		{
			sInfoList = new CMList<CMSendFileInfo>();
			m_sendFileHashtable.put(uName, sInfoList);
		}
		
		bResult = sInfoList.addElement(sInfo);
		if(!bResult)
		{
			System.err.println("CMFileTransferInfo.addSendFileInfo() failed: "+sInfo.toString());
			return false;
		}
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileTransferInfo.addSendFileInfo() done: "+sInfo.toString());
			System.out.println("# current hashtable elements: "+m_sendFileHashtable.size());
		}

		return true;
	}

	public boolean addSendFileInfo(CMSendFileInfo sInfo)
	{
		String strFileName = null;
		CMList<CMSendFileInfo> sInfoList = null;
		boolean bResult = false;
		
		strFileName = sInfo.getFilePath().substring(sInfo.getFilePath().lastIndexOf(File.separator)+1);
		sInfo.setFileName(strFileName);
		
		sInfoList = m_sendFileHashtable.get(sInfo.getReceiverName());
		if(sInfoList == null)
		{
			sInfoList = new CMList<CMSendFileInfo>();
			m_sendFileHashtable.put(sInfo.getReceiverName(), sInfoList);
		}
		
		bResult = sInfoList.addElement(sInfo);
		if(!bResult)
		{
			System.err.println("CMFileTransferInfo.addSendFileInfo() failed: "+sInfo.toString());
			return false;
		}
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileTransferInfo.addSendFileInfo() done: "+sInfo.toString());
			System.out.println("# current hashtable elements: "+m_sendFileHashtable.size());
		}

		return true;		
	}

	public CMSendFileInfo findSendFileInfo(String uName, String fName, int nContentID)
	{
		CMSendFileInfo sInfo = null;
		CMList<CMSendFileInfo> sInfoList = null;
		CMSendFileInfo tInfo = null;
		
		sInfoList = m_sendFileHashtable.get(uName);
		if(sInfoList == null)
		{
			System.err.println("CMFileTransferInfo.findSendFileInfo(), list not found for receiver("
					+uName+")");
			return null;
		}
		
		tInfo = new CMSendFileInfo();
		tInfo.setReceiverName(uName);
		tInfo.setFileName(fName);
		tInfo.setContentID(nContentID);
		
		sInfo = sInfoList.findElement(tInfo);
		
		if(sInfo == null)
		{
			System.err.println("CMFileTransferInfo.findSendFileInfo(), not found!: "+tInfo.toString());
			return null;
		}
		
		return sInfo;
	}

	public boolean removeSendFileInfo(String uName, String fName, int nContentID)
	{
		CMList<CMSendFileInfo> sInfoList = null;
		CMSendFileInfo sInfo = null;
		boolean bResult = false;

		sInfoList = m_sendFileHashtable.get(uName);
		if(sInfoList == null)
		{
			System.err.println("CMFileTransferInfo.removeSendFileInfo(), list not found for receiver("
					+uName+")");
			return false;
		}
		
		sInfo = new CMSendFileInfo();
		sInfo.setReceiverName(uName);
		sInfo.setFileName(fName);
		sInfo.setContentID(nContentID);
		bResult = sInfoList.removeElement(sInfo);

		if(!bResult)
		{
			System.err.println("CMFileTransferInfo.removeSendFileInfo() error! : "+sInfo.toString());
			return false;
		}
		
		if(sInfoList.isEmpty())
		{
			m_sendFileHashtable.remove(uName);
		}

		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileTransferInfo.removeSendFileInfo() done : "+sInfo.toString());
			System.out.println("# current hashtable elements: "+m_sendFileHashtable.size());
		}
		
		return true;
		
	}
	
	public Hashtable<String, CMList<CMSendFileInfo>> getSendFileHashtable()
	{
		return m_sendFileHashtable;
	}

	////////// add/remove/find receiving file info

	public boolean addRecvFileInfo(String senderName, String fName, long lSize, int nContentID,
			long lRecvSize, RandomAccessFile writeFile)
	{
		CMRecvFileInfo rInfo = null;
		CMList<CMRecvFileInfo> rInfoList = null;
		boolean bResult = false;

		rInfo = null;
		rInfo = new CMRecvFileInfo();
		rInfo.setSenderName(senderName);
		rInfo.setFileName(fName);
		rInfo.setFileSize(lSize);
		rInfo.setContentID(nContentID);
		rInfo.setRecvSize(lRecvSize);
		rInfo.setWriteFile(writeFile);
		
		rInfoList = m_recvFileHashtable.get(senderName);
		if(rInfoList == null)
		{
			rInfoList = new CMList<CMRecvFileInfo>();
			m_recvFileHashtable.put(senderName, rInfoList);
		}
		
		bResult = rInfoList.addElement(rInfo);
		if(!bResult)
		{
			System.err.println("CMFileTransferInfo.addRecvFileInfo() failed: "+rInfo.toString());
			return false;
		}
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileTransferInfo.addRecvFileInfo() done: "+rInfo.toString());
			System.out.println("# current hashtable elements: "+m_recvFileHashtable.size());
		}
		
		return true;
	}
	
	public boolean addRecvFileInfo(CMRecvFileInfo rInfo)
	{
		CMList<CMRecvFileInfo> rInfoList = null;
		boolean bResult = false;
		
		rInfoList = m_recvFileHashtable.get(rInfo.getSenderName());
		if(rInfoList == null)
		{
			rInfoList = new CMList<CMRecvFileInfo>();
			m_recvFileHashtable.put(rInfo.getSenderName(), rInfoList);
		}
		
		bResult = rInfoList.addElement(rInfo);
		if(!bResult)
		{
			System.err.println("CMFileTransferInfo.addRecvFileInfo() failed: "+rInfo.toString());
			return false;
		}
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileTransferInfo.addRecvFileInfo() done: "+rInfo.toString());
			System.out.println("# current hashtable elements: "+m_recvFileHashtable.size());
		}
		
		return true;		
	}

	public CMRecvFileInfo findRecvFileInfo(String senderName, String fName, int nContentID)
	{	
		CMRecvFileInfo rInfo = null;
		CMList<CMRecvFileInfo> rInfoList = null;
		CMRecvFileInfo tInfo = null;
		
		rInfoList = m_recvFileHashtable.get(senderName);
		if(rInfoList == null)
		{
			System.err.println("CMFileTransferInfo.findRecvFileInfo(), list not found for sender("
					+senderName+")");
			return null;
		}
		
		tInfo = new CMRecvFileInfo();
		tInfo.setSenderName(senderName);
		tInfo.setFileName(fName);
		tInfo.setContentID(nContentID);
		
		rInfo = rInfoList.findElement(tInfo);
		
		if(rInfo == null)
		{
			System.err.println("CMFileTransferInfo.findRecvFileInfo(), not found!: "+tInfo.toString());
			return null;
		}
				
		return rInfo;

	}

	public boolean removeRecvFileInfo(String senderName, String fName, int nContentID)
	{
		CMList<CMRecvFileInfo> rInfoList = null;
		CMRecvFileInfo rInfo = null;
		boolean bResult = false;

		rInfoList = m_recvFileHashtable.get(senderName);
		if(rInfoList == null)
		{
			System.err.println("CMFileTransferInfo.removeRecvFileInfo(), list not found for sender("
					+senderName+")");
			return false;
		}
		
		rInfo = new CMRecvFileInfo();
		rInfo.setSenderName(senderName);
		rInfo.setFileName(fName);
		rInfo.setContentID(nContentID);
		bResult = rInfoList.removeElement(rInfo);

		if(!bResult)
		{
			System.err.println("CMFileTransferInfo.removeRecvFileInfo() error! : "+rInfo.toString());
			return false;
		}
		
		if(rInfoList.isEmpty())
		{
			m_sendFileHashtable.remove(senderName);
		}

		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileTransferInfo.removeSendFileInfo() done : "+rInfo.toString());
			System.out.println("# current hashtable elements: "+m_sendFileHashtable.size());
		}
		
		return true;
		
	}
	
	public Hashtable<String, CMList<CMRecvFileInfo>> getRecvFileHashtable()
	{
		return m_recvFileHashtable;
	}
	
	////// find the receiving file info that is being used or is not yet started by the thread pool
	
	public CMRecvFileInfo findRecvFileInfoNotStarted(String strSender)
	{
		CMRecvFileInfo rfInfo = null;
		CMList<CMRecvFileInfo> rfInfoList = m_recvFileHashtable.get(strSender);
		boolean bFound = false;
		
		if(rfInfoList == null) return null;
		
		Iterator<CMRecvFileInfo> iter = rfInfoList.getList().iterator();
		while(iter.hasNext() && !bFound)
		{
			rfInfo = iter.next();
			if(rfInfo.getRecvTaskResult() == null)
			{
				bFound = true;
				if(CMInfo._CM_DEBUG)
					System.out.println("CMFileTransferInfo.findRecvFileInfoNotStarted(); found: "+rfInfo.toString());
			}
		}
		
		if(bFound)
			return rfInfo;
		else
		{
			if(CMInfo._CM_DEBUG)
				System.out.println("CMFileTransferInfo.findRecvFileInfoNotStarted(); not found!");
			return null;
		}
	}
	
	public boolean isRecvOngoing(String strSender)
	{
		CMRecvFileInfo rfInfo = null;
		CMList<CMRecvFileInfo> rfInfoList = m_recvFileHashtable.get(strSender);
		boolean bFound = false;
		
		if(rfInfoList == null) return false;
		
		Iterator<CMRecvFileInfo> iter = rfInfoList.getList().iterator();
		while(iter.hasNext() && !bFound)
		{
			rfInfo = iter.next();
			if(rfInfo.getRecvTaskResult() != null)
			{
				bFound = true;
				if(CMInfo._CM_DEBUG)
					System.out.println("CMFileTransferInfo.isRecvOngoing(); ongoing recv info found: "+rfInfo.toString());
			}
		}
		
		return bFound;
	}
	
}
