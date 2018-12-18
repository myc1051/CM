package kr.ac.konkuk.ccslab.cm.manager;
import java.io.*;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import kr.ac.konkuk.ccslab.cm.entity.CMRecvVideoInfo;
import kr.ac.konkuk.ccslab.cm.entity.CMSendVideoInfo;
import kr.ac.konkuk.ccslab.cm.entity.CMServer;
import kr.ac.konkuk.ccslab.cm.entity.CMChannelInfo;
import kr.ac.konkuk.ccslab.cm.entity.CMMessage;
import kr.ac.konkuk.ccslab.cm.entity.CMUser;
import kr.ac.konkuk.ccslab.cm.event.CMVideoEvent;
import kr.ac.konkuk.ccslab.cm.info.CMCommInfo;
import kr.ac.konkuk.ccslab.cm.info.CMConfigurationInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInteractionInfo;
import kr.ac.konkuk.ccslab.cm.info.CMStreamingInfo;
import kr.ac.konkuk.ccslab.cm.thread.CMRecvVideoTask;
import kr.ac.konkuk.ccslab.cm.thread.CMSendVideoTask;

public class CMStreamingManager {

	public static void init(CMInfo cmInfo)
	{
		CMConfigurationInfo confInfo = cmInfo.getConfigurationInfo();
		CMStreamingInfo fInfo = cmInfo.getStreamingInfo();
		String strPath = confInfo.getVideoPath();
		
		// if the default directory does not exist, create it.
		File defaultPath = new File(strPath);
		if(!defaultPath.exists() || !defaultPath.isDirectory())
		{
			boolean ret = defaultPath.mkdirs();
			if(ret)
			{
				if(CMInfo._CM_DEBUG)
					System.out.println("A default path is created!");
			}
			else
			{
				System.out.println("A default path cannot be created!");
				return;
			}
		}
		
		fInfo.setVideoPath(strPath);
		if(CMInfo._CM_DEBUG)
			System.out.println("A default path for the Video Streaming: "+strPath);
		
		// create an executor service object
		ExecutorService es = fInfo.getExecutorService();
		int nAvailableProcessors = Runtime.getRuntime().availableProcessors();
		es = Executors.newFixedThreadPool(nAvailableProcessors);
		fInfo.setExecutorService(es);
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMStreamingManager.init(), executor service created; # available processors("
					+nAvailableProcessors+").");
		}
				
		return;
	}
	
	public static void terminate(CMInfo cmInfo)
	{
		CMStreamingInfo fInfo = cmInfo.getStreamingInfo();
		ExecutorService es = fInfo.getExecutorService();
		es.shutdown();	// need to check
	}
	
	public static void setVideoPath(String strVideoPath, CMInfo cmInfo)
	{
		CMStreamingInfo fInfo = cmInfo.getStreamingInfo();
		fInfo.setVideoPath(strVideoPath);
		return;
	}
	
	public static void requestVideo(String strVideoName, String strVideoOwner, CMInfo cmInfo)
	{
		requestVideo(strVideoName, strVideoOwner, -1, cmInfo);
		return;
	}
	
	public static void requestVideo(String strVideoName, String strVideoOwner, int nContentID, CMInfo cmInfo)
	{
		CMConfigurationInfo confInfo = cmInfo.getConfigurationInfo();
		CMUser myself = cmInfo.getInteractionInfo().getMyself();
		
		if(confInfo.getSystemType().equals("CLIENT") && myself.getState() != CMInfo.CM_LOGIN 
				&& myself.getState() != CMInfo.CM_SESSION_JOIN)
		{
			System.err.println("CMStreamingManager.requestVideo(), Client must log in to the default server.");
			return;
		}
		
		requestVideoWithSepChannel(strVideoName, strVideoOwner, nContentID, cmInfo);
		return;
	}
	
	public static void requestVideoWithDefChannel(String strVideoName, String strVideoOwner, int nContentID, CMInfo cmInfo)
	{
		CMUser myself = cmInfo.getInteractionInfo().getMyself();
		
		CMVideoEvent fe = new CMVideoEvent();
		fe.setID(CMVideoEvent.REQUEST_VIDEO_STREAMING);
		fe.setUserName(myself.getName());	// requester name
		fe.setVideoName(strVideoName);
		fe.setContentID(nContentID);
		CMEventManager.unicastEvent(fe, strVideoOwner, cmInfo);
		
		fe = null;
		return;
	}
	
	public static void requestVideoWithSepChannel(String strVideoName, String strVideoOwner, int nContentID, CMInfo cmInfo)
	{
		CMUser myself = cmInfo.getInteractionInfo().getMyself();
		
		CMVideoEvent fe = new CMVideoEvent();
		fe.setID(CMVideoEvent.REQUEST_VIDEO_STREAMING_CHAN);
		fe.setUserName(myself.getName());	// requester name
		fe.setVideoName(strVideoName);
		fe.setContentID(nContentID);
		CMEventManager.unicastEvent(fe, strVideoOwner, cmInfo);
		
		fe = null;
		return;
	}
	
	
	public static void pushVideo(String strVideoPath, String strReceiver, CMInfo cmInfo)
	{
		pushVideo(strVideoPath, strReceiver, -1, cmInfo);
		return;
	}
	
	public static void pushVideo(String strVideoPath, String strReceiver, int nContentID, CMInfo cmInfo)
	{
		CMConfigurationInfo confInfo = cmInfo.getConfigurationInfo();
		CMUser myself = cmInfo.getInteractionInfo().getMyself();
		if(confInfo.getSystemType().equals("CLIENT") && myself.getState() != CMInfo.CM_LOGIN 
				&& myself.getState() != CMInfo.CM_SESSION_JOIN)
		{
			System.err.println("CMVideoTransferManager.pushVideo(), Client must log in to the default server.");
			return;
		}
		
		pushVideoWithSepChannel(strVideoPath, strReceiver, nContentID, cmInfo);
		return;
	}

	// strVideoPath: absolute or relative path to a target Video
	public static void pushVideoWithDefChannel(String strVideoPath, String strReceiver, int nContentID, CMInfo cmInfo)
	{
		CMStreamingInfo fInfo = cmInfo.getStreamingInfo();
		CMInteractionInfo interInfo = cmInfo.getInteractionInfo();
		
		// get Video information (size)
		File Video = new File(strVideoPath);
		if(!Video.exists())
		{
			System.err.println("CMVideoTransferManager.pushVideo(), Video("+strVideoPath+") does not exists.");
			return;
		}
		long lVideoSize = Video.length();
		
		// add send Video information
		// receiver name, Video path, size
		fInfo.addSendVideoInfo(strReceiver, strVideoPath, lVideoSize, nContentID);

		// get my name
		String strMyName = interInfo.getMyself().getName();

		// get Video name
		String strVideoName = getVideoNameFromPath(strVideoPath);
		System.out.println("Video name: "+strVideoName);
		
		// start Video transfer process
		CMVideoEvent fe = new CMVideoEvent();
		fe.setID(CMVideoEvent.START_VIDEO_STREAMING);
		fe.setSenderName(strMyName);
		fe.setVideoName(strVideoName);
		fe.setVideoSize(lVideoSize);
		fe.setContentID(nContentID);
		CMEventManager.unicastEvent(fe, strReceiver, cmInfo);

		Video = null;
		fe = null;
		return;
	}
	
	// strVideoPath: absolute or relative path to a target Video
	public static void pushVideoWithSepChannel(String strVideoPath, String strReceiver, int nContentID, CMInfo cmInfo)
	{
		CMStreamingInfo sInfo = cmInfo.getStreamingInfo();
		CMInteractionInfo interInfo = cmInfo.getInteractionInfo();
		CMConfigurationInfo confInfo = cmInfo.getConfigurationInfo();

		// check the creation of the default blocking TCP socket channel
		CMChannelInfo<Integer> blockChannelList = null;
		CMChannelInfo<Integer> nonBlockChannelList = null;
		SocketChannel sc = null;
		SocketChannel dsc = null;
		if(confInfo.getSystemType().equals("CLIENT"))
		{
			blockChannelList = interInfo.getDefaultServerInfo().getBlockSocketChannelInfo();
			sc = (SocketChannel) blockChannelList.findChannel(0);	// default key for the blocking channel is 0
			nonBlockChannelList = interInfo.getDefaultServerInfo().getNonBlockSocketChannelInfo();
			dsc = (SocketChannel) nonBlockChannelList.findChannel(0); // key for the default TCP socket channel is 0
		}
		else	// SERVER
		{
			CMUser user = interInfo.getLoginUsers().findMember(strReceiver);
			blockChannelList = user.getBlockSocketChannelInfo();
			sc = (SocketChannel) blockChannelList.findChannel(0);	// default key for the blocking channel is 0
			nonBlockChannelList = user.getNonBlockSocketChannelInfo();
			dsc = (SocketChannel) nonBlockChannelList.findChannel(0);	// key for the default TCP socket channel is 0
		}

		if(sc == null)
		{
			System.err.println("CMVideoTransferManager.pushVideoWithSepChannel(); "
					+ "default blocking TCP socket channel not found!");
			return;
		}
		else if(!sc.isOpen())
		{
			System.err.println("CMVideoTransferManager.pushVideoWithSepChannel(); "
					+ "default blocking TCP socket channel closed!");
			return;
		}
		
		if(dsc == null)
		{
			System.err.println("CMVideoTransferManager.pushVideoWithSepChannel(); "
					+ "default TCP socket channel not found!");
			return;
		}
		else if(!dsc.isOpen())
		{
			System.err.println("CMVideoTransferManager.pushVideoWithSepChannel(); "
					+ "default TCP socket channel closed!");
			return;
		}


		// get Video information (size)
		File Video = new File(strVideoPath);
		if(!Video.exists())
		{
			System.err.println("CMVideoTransferManager.pushVideoWithSepChannel(), Video("+strVideoPath+") does not exists.");
			return;
		}
		long lVideoSize = Video.length();
		
		// add send Video information
		// sender name, receiver name, Video path, size, content ID
		CMSendVideoInfo svInfo = new CMSendVideoInfo();
		svInfo.setSenderName(interInfo.getMyself().getName());
		svInfo.setReceiverName(strReceiver);
		svInfo.setVideoPath(strVideoPath);
		svInfo.setVideoSize(lVideoSize);
		svInfo.setContentID(nContentID);
		svInfo.setSendChannel(sc);
		svInfo.setDefaultChannel(dsc);
		//boolean bResult = fInfo.addSendVideoInfo(strReceiver, strVideoPath, lVideoSize, nContentID);
		boolean bResult = sInfo.addSendVideoInfo(svInfo);
		if(!bResult)
		{
			System.err.println("CMVideoTransferManager.pushVideoWithSepChannel(); error for adding the sending Video info: "
					+"receiver("+strReceiver+"), Video("+strVideoPath+"), size("+lVideoSize+"), content ID("
					+nContentID+")!");
			return;
		}

		// get my name
		String strMyName = interInfo.getMyself().getName();

		// get Video name
		String strVideoName = getVideoNameFromPath(strVideoPath);
		
		// start Video transfer process
		CMVideoEvent fe = new CMVideoEvent();
		fe.setID(CMVideoEvent.START_VIDEO_STREAMING_CHAN);
		fe.setSenderName(strMyName);
		fe.setVideoName(strVideoName);
		fe.setVideoSize(lVideoSize);
		fe.setContentID(nContentID);
		CMEventManager.unicastEvent(fe, strReceiver, cmInfo);

		Video = null;
		fe = null;
		return;
	}


	// srcVideo: reference of RandomAccessVideo of source Video
	// bos: reference of BufferedOutputStream of split Video
	public static void splitVideo(RandomAccessFile srcVideo, long lOffset, long lSplitSize, String strSplitVideo)
	{
		long lRemainBytes = lSplitSize;
		byte[] VideoBlock = new byte[CMInfo.VIDEO_BLOCK_LEN];
		int readBytes;
		BufferedOutputStream bos = null;
		try {
			bos = new BufferedOutputStream(new FileOutputStream(strSplitVideo));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			// set Video position
			srcVideo.seek(lOffset);

			// read and write
			while( lRemainBytes > 0 )
			{
				if(lRemainBytes >= CMInfo.VIDEO_BLOCK_LEN)
					readBytes = srcVideo.read(VideoBlock);
				else
					readBytes = srcVideo.read(VideoBlock, 0, (int)lRemainBytes);

				if( readBytes >= CMInfo.VIDEO_BLOCK_LEN )
					bos.write(VideoBlock);
				else
					bos.write(VideoBlock, 0, readBytes);

				lRemainBytes -= readBytes;
			}
			
			bos.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return;
	}
	
	public static long mergeVideos(String[] strSplitVideos, int nSplitNum, String strMergeVideo)
	{
		long lMergeSize = -1;
		long lSrcSize = 0;
		FileInputStream srcfis = null;
		BufferedOutputStream bos = null;
		byte[] VideoBlock = new byte[CMInfo.VIDEO_BLOCK_LEN];
		int readBytes = 0;

		if(nSplitNum != strSplitVideos.length)
		{
			System.err.println("CMVideoTransferManager.mergeVideos(), the number of members in the "
					+"first parameter is different from the given second parameter!");
			return -1;
		}
		
		// open a target Video
		try {
			
			bos = new BufferedOutputStream(new FileOutputStream(strMergeVideo));
			
			for(int i = 0; i < nSplitNum; i++)
			{
				// open a source Video
				File srcVideo = new File(strSplitVideos[i]);
				srcfis = new FileInputStream(srcVideo);

				// get source Video size
				lSrcSize = srcVideo.length();

				// concatenate a source Video to a target Video
				while( lSrcSize > 0 )
				{
					if( lSrcSize >= CMInfo.VIDEO_BLOCK_LEN )
					{
						readBytes = srcfis.read(VideoBlock);
						bos.write(VideoBlock, 0, readBytes);
					}
					else
					{
						readBytes = srcfis.read(VideoBlock, 0, (int)lSrcSize);
						bos.write(VideoBlock, 0, readBytes);
					}

					lSrcSize -= readBytes;
				}

				// close a source Video
				srcfis.close();
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if(srcfis != null)
				{
					srcfis.close();
				}
				if(bos != null){
					bos.close();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		File targetVideo = new File(strMergeVideo);
		lMergeSize = targetVideo.length();
		
		return lMergeSize;
	}
	
	public static String getVideoNameFromPath(String strPath)
	{
		String strName = null;
		int index;
		String sep = File.separator;
		/*
		index = strPath.lastIndexOf("/");
		if(index == -1)
		{
			index = strPath.lastIndexOf(sep);
			if(index == -1)
				strName = strPath;
			else
				strName = strPath.substring(index+1);
		}
		else
		{
			strName = strPath.substring(index+1);
		}
		*/
		index = strPath.lastIndexOf(sep);
		if(index == -1)
			strName = strPath;
		else
			strName = strPath.substring(index+1);
		
		return strName;
	}
	
	//////////////////////////////////////////////////////////////////
	// process Video event
	
	public static void processEvent(CMMessage msg, CMInfo cmInfo)
	{
		CMVideoEvent fe = new CMVideoEvent(msg.m_buf);
		
		switch(fe.getID())
		{
		case CMVideoEvent.REQUEST_VIDEO_STREAMING:
			processREQUEST_VIDEO_STREAMING(fe, cmInfo);
			break;
		case CMVideoEvent.REPLY_VIDEO_STREAMING:
			processREPLY_VIDEO_STREAMING(fe, cmInfo);
			break;
		case CMVideoEvent.START_VIDEO_STREAMING:
			processSTART_VIDEO_STREAMING(fe, cmInfo);
			break;
		case CMVideoEvent.START_VIDEO_STREAMING_ACK:
			processSTART_VIDEO_STREAMING_ACK(fe, cmInfo);
			break;
		case CMVideoEvent.CONTINUE_VIDEO_STREAMING:
			processCONTINUE_VIDEO_STREAMING(fe, cmInfo);
			break;
		case CMVideoEvent.END_VIDEO_STREAMING:
			processEND_VIDEO_STREAMING(fe, cmInfo);
			break;
		case CMVideoEvent.END_VIDEO_STREAMING_ACK:
			processEND_VIDEO_STREAMING_ACK(fe, cmInfo);
			break;
		case CMVideoEvent.REQUEST_DIST_VIDEO_PROC:
			processREQUEST_DIST_Video_PROC(fe, cmInfo);
			break;
		case CMVideoEvent.REQUEST_VIDEO_STREAMING_CHAN:
			processREQUEST_VIDEO_STREAMING_CHAN(fe, cmInfo);
			break;
		case CMVideoEvent.REPLY_VIDEO_STREAMING_CHAN:
			processREPLY_VIDEO_STREAMING_CHAN(fe, cmInfo);
			break;
		case CMVideoEvent.START_VIDEO_STREAMING_CHAN:
			processSTART_VIDEO_STREAMING_CHAN(fe, cmInfo);
			break;
		case CMVideoEvent.START_VIDEO_STREAMING_CHAN_ACK:
			processSTART_VIDEO_STREAMING_CHAN_ACK(fe, cmInfo);
			break;
		case CMVideoEvent.END_VIDEO_STREAMING_CHAN:
			processEND_VIDEO_STREAMING_CHAN(fe, cmInfo);
			break;
		case CMVideoEvent.END_VIDEO_STREAMING_CHAN_ACK:
			processEND_VIDEO_STREAMING_CHAN_ACK(fe, cmInfo);
			break;
		default:
			System.err.println("CMVideoTransferManager.processEvent(), unknown event id("+fe.getID()+").");
			fe = null;
			return;
		}
		
		fe = null;
		return;
	}
	
	private static void processREQUEST_VIDEO_STREAMING(CMVideoEvent fe, CMInfo cmInfo)
	{
		CMStreamingInfo fInfo = cmInfo.getStreamingInfo();
		CMUser myself = cmInfo.getInteractionInfo().getMyself();
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMVideoTransferManager.processREQUEST_VIDEO_STREAMING(), requester("
					+fe.getUserName()+"), Video("+fe.getVideoName()+"), contentID("+fe.getContentID()
					+").");
		}

		String strVideoName = fe.getVideoName();
		CMVideoEvent feAck = new CMVideoEvent();
		feAck.setID(CMVideoEvent.REPLY_VIDEO_STREAMING);
		feAck.setVideoName(strVideoName);

		// get the full path of the requested Video
		String strFullPath = fInfo.getVideoPath() + File.separator + strVideoName; 
		// check the Video existence
		File Video = new File(strFullPath);
		if(!Video.exists())
		{
			feAck.setReturnCode(0);	// Video not found
			CMEventManager.unicastEvent(feAck, fe.getUserName(), cmInfo);
			feAck = null;
			return;
		}
		
		feAck.setReturnCode(1);	// Video found
		feAck.setContentID(fe.getContentID());
		CMEventManager.unicastEvent(feAck, fe.getUserName(), cmInfo);
		
		// get the Video size
		long lVideoSize = Video.length();
		
		// add send Video information
		// receiver name, Video path, size
		fInfo.addSendVideoInfo(fe.getUserName(), strFullPath, lVideoSize, fe.getContentID());

		// start Video transfer process
		CMVideoEvent feStart = new CMVideoEvent();
		feStart.setID(CMVideoEvent.START_VIDEO_STREAMING);
		feStart.setSenderName(myself.getName());
		feStart.setVideoName(fe.getVideoName());
		feStart.setVideoSize(lVideoSize);
		feStart.setContentID(fe.getContentID());
		CMEventManager.unicastEvent(feStart, fe.getUserName(), cmInfo);

		feAck = null;
		feStart = null;
		Video = null;
		return;
	}
	
	private static void processREPLY_VIDEO_STREAMING(CMVideoEvent fe, CMInfo cmInfo)
	{
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMVideoManager.processREPLY_VIDEO_STREAMING(), Video("+fe.getVideoName()
					+"), return code("+fe.getReturnCode()+"), contentID("+fe.getContentID()+").");
		}
		return;
	}
	
	private static void processSTART_VIDEO_STREAMING(CMVideoEvent fe, CMInfo cmInfo)
	{
		CMStreamingInfo fInfo = cmInfo.getStreamingInfo();
		CMConfigurationInfo confInfo = cmInfo.getConfigurationInfo();
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMVideoTransferManager.processSTART_VIDEO_STREAMING(),");
			System.out.println("sender("+fe.getSenderName()+"), Video("+fe.getVideoName()+"), size("
					+fe.getVideoSize()+"), contentID("+fe.getContentID()+").");
		}
		
		// set Video size
		long lVideoSize = fe.getVideoSize();
		
		// set a path of the received Video
		String strFullPath = fInfo.getVideoPath();
		if(confInfo.getSystemType().equals("CLIENT"))
		{
			strFullPath = strFullPath + File.separator + fe.getVideoName();
		}
		else if(confInfo.getSystemType().equals("SERVER"))
		{
			// check the sub-directory and create it if it does not exist
			strFullPath = strFullPath + File.separator + fe.getSenderName();
			File subDir = new File(strFullPath);
			if(!subDir.exists() || !subDir.isDirectory())
			{
				boolean ret = subDir.mkdirs();
				if(ret)
				{
					if(CMInfo._CM_DEBUG)
						System.out.println("A sub-directory is created.");
				}
				else
				{
					System.out.println("A sub-directory cannot be created!");
					return;
				}
			}
			
			strFullPath = strFullPath + File.separator + fe.getVideoName();
		}
		else
		{
			System.err.println("Wrong system type!");
			return;
		}
		
		
		// open a Video output stream
		RandomAccessFile writeVideo;
		try {
			writeVideo = new RandomAccessFile(strFullPath, "rw");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		
		
		// init received size
		long lRecvSize = 0;
		// add the received Video info in the push list
		fInfo.addRecvVideoInfo(fe.getSenderName(), fe.getVideoName(), lVideoSize, fe.getContentID(), lRecvSize, writeVideo);
		
		// send ack event
		CMVideoEvent feAck = new CMVideoEvent();
		feAck.setID(CMVideoEvent.START_VIDEO_STREAMING_ACK);
		feAck.setUserName(cmInfo.getInteractionInfo().getMyself().getName());
		feAck.setVideoName(fe.getVideoName());
		feAck.setContentID(fe.getContentID());
		CMEventManager.unicastEvent(feAck, fe.getSenderName(), cmInfo);

		feAck = null;
		return;
	}
	
	private static void processSTART_VIDEO_STREAMING_ACK(CMVideoEvent recvVideoEvent, CMInfo cmInfo)
	{
		String strReceiver = null;
		String strVideoName = null;
		String strFullVideoName = null;
		long lVideoSize = -1;
		int nContentID = -1;
		String strSenderName = null;
		CMStreamingInfo fInfo = cmInfo.getStreamingInfo();
		CMSendVideoInfo sInfo = null;
		
		// find the CMSendVideoInfo object 
		sInfo = fInfo.findSendVideoInfo(recvVideoEvent.getUserName(), recvVideoEvent.getVideoName(), 
				recvVideoEvent.getContentID());
		if(sInfo == null)
		{
			System.err.println("CMVideoTransferManager.processSTART_VIDEO_STREAMING_ACK(), sendVideoInfo not found! : "
					+"receiver("+recvVideoEvent.getUserName()+"), Video("+recvVideoEvent.getVideoName()
					+"), content ID("+recvVideoEvent.getContentID()+")");
			return;
		}
		
		strReceiver = sInfo.getReceiverName();
		strFullVideoName = sInfo.getVideoPath();
		strVideoName = getVideoNameFromPath(strFullVideoName);
		lVideoSize = sInfo.getVideoSize();
		nContentID = sInfo.getContentID();
					
		if(CMInfo._CM_DEBUG)
			System.out.println("CMVideoTransferManager.processSTART_VIDEO_STREAMING_ACK(), "
					+ "Sending Video("+strVideoName+") to target("+strReceiver+").");

		// open the Video
		try {
			sInfo.setReadVideo(new RandomAccessFile(strFullVideoName, "rw"));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		
		// set sender name
		strSenderName = cmInfo.getInteractionInfo().getMyself().getName();

		// send blocks
		long lRemainBytes = lVideoSize;
		int nReadBytes = 0;
		byte[] VideoBlock = new byte[CMInfo.VIDEO_BLOCK_LEN];
		CMVideoEvent fe = new CMVideoEvent();
		
		while(lRemainBytes > 0)
		{
			try {
				nReadBytes = sInfo.getReadVideo().read(VideoBlock);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			// send Video block
			fe = new CMVideoEvent();
			fe.setID(CMVideoEvent.CONTINUE_VIDEO_STREAMING);
			fe.setSenderName(strSenderName);
			fe.setVideoName(strVideoName);
			fe.setVideoBlock(VideoBlock);
			fe.setBlockSize(nReadBytes);
			fe.setContentID(nContentID);
			CMEventManager.unicastEvent(fe, strReceiver, cmInfo);
			
			lRemainBytes -= nReadBytes;
		}
		
		// close fis
		try {
			sInfo.getReadVideo().close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if(CMInfo._CM_DEBUG)
			System.out.println("CMVideoTransferManager.processSTART_VIDEO_STREAMING_ACK(), "
					+ "Ending transfer of Video("+strVideoName+") to target("+strReceiver
					+"), size("+lVideoSize+") Bytes.");

		// send the end of Video transfer
		fe = new CMVideoEvent();
		fe.setID(CMVideoEvent.END_VIDEO_STREAMING);
		fe.setSenderName(strSenderName);
		fe.setVideoName(strVideoName);
		fe.setVideoSize(lVideoSize);
		fe.setContentID(nContentID);
		CMEventManager.unicastEvent(fe, strReceiver, cmInfo);
		
		fe = null;
		return;
	}
	
	private static void processCONTINUE_VIDEO_STREAMING(CMVideoEvent fe, CMInfo cmInfo)
	{
		CMStreamingInfo fInfo = cmInfo.getStreamingInfo();
		
		/*
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMVideoManager.processCONTINUE_VIDEO_STREAMING(), sender("
					+fe.getSenderName()+"), Video("+fe.getVideoName()+"), "+fe.getBlockSize()
					+" Bytes, contentID("+fe.getContentID()+").");
		}
		*/

		// find info in the recv Video list
		CMRecvVideoInfo recvInfo = fInfo.findRecvVideoInfo(fe.getSenderName(), fe.getVideoName(), fe.getContentID());
		if( recvInfo == null )
		{
			System.err.println("CMVideoTransferManager.processCONTINUE_VIDEO_STREAMING(), "
					+ "recv Video info for sender("+fe.getSenderName()+"), Video("+fe.getVideoName()
					+"), content ID("+fe.getContentID()+") not found.");
			return;
		}

		try {
			recvInfo.getWriteVideo().write(fe.getVideoBlock(), 0, fe.getBlockSize());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		recvInfo.setRecvSize(recvInfo.getRecvSize()+fe.getBlockSize());

		/*
		if(CMInfo._CM_DEBUG)
			System.out.println("Cumulative written Video size: "+pushInfo.m_lRecvSize+" Bytes.");
		*/
		
		return;
	}
	
	private static void processEND_VIDEO_STREAMING(CMVideoEvent fe, CMInfo cmInfo)
	{
		CMStreamingInfo fInfo = cmInfo.getStreamingInfo();
		CMInteractionInfo interInfo = cmInfo.getInteractionInfo();
		
		// find info from recv Video list
		CMRecvVideoInfo recvInfo = fInfo.findRecvVideoInfo(fe.getSenderName(), fe.getVideoName(), fe.getContentID());
		if(recvInfo == null)
		{
			System.err.println("CMVideoTransferManager.processEND_VIDEO_STREAMING(), recv Video info "
					+"for sender("+fe.getSenderName()+"), Video("+fe.getVideoName()+"), content ID("
					+fe.getContentID()+") not found.");
			return;
		}
		// close received Video descriptor
		try {
			recvInfo.getWriteVideo().close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMVideoTransferManager.processEND_VIDEO_STREAMING(), sender("+fe.getSenderName()
					+"), Video("+fe.getVideoName()+"), contentID("+fe.getContentID()+"), Video size("
					+recvInfo.getVideoSize()+"), received size("+recvInfo.getRecvSize()+").");
		}

		// remove info from push Video list
		fInfo.removeRecvVideoInfo(fe.getSenderName(), fe.getVideoName(), fe.getContentID());
		
		// send ack
		CMVideoEvent feAck = new CMVideoEvent();
		feAck.setID(CMVideoEvent.END_VIDEO_STREAMING_ACK);
		feAck.setUserName(interInfo.getMyself().getName());
		feAck.setVideoName(fe.getVideoName());
		feAck.setReturnCode(1);	// success
		feAck.setContentID(fe.getContentID());
		CMEventManager.unicastEvent(feAck, fe.getSenderName(), cmInfo);		
		feAck = null;
		
		//CMSNSManager.checkCompleteRecvAttachedVideos(fe, cmInfo);

		return;
	}
	
	private static void processEND_VIDEO_STREAMING_ACK(CMVideoEvent fe, CMInfo cmInfo)
	{
		CMStreamingInfo fInfo = cmInfo.getStreamingInfo();
		String strReceiverName = fe.getUserName();
		String strVideoName = fe.getVideoName();
		int nContentID = fe.getContentID();
		
		// find completed send info
		CMSendVideoInfo sInfo = fInfo.findSendVideoInfo(strReceiverName, strVideoName, nContentID);
		if(sInfo == null)
		{
			System.err.println("CMVideoTransferManager.processEND_VIDEO_STREAMING_ACK(), send info not found");
			System.err.println("receiver("+strReceiverName+"), Video("+strVideoName+"), content ID("+nContentID+").");
		}
		else
		{
			// delete corresponding request from the list
			fInfo.removeSendVideoInfo(strReceiverName, strVideoName, nContentID);
		}
	
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMVideoTransferManager.processEND_VIDEO_STREAMING_ACK(), receiver("
					+strReceiverName+"), Video("+strVideoName+"), return code("+fe.getReturnCode()
					+"), contentID("+nContentID+").");
		}
		
		//////////////////// check the completion of sending attached Video of SNS content
		//////////////////// and check the completion of prefetching an attached Video of SNS content
		//CMSNSManager.checkCompleteSendAttachedVideos(fe, cmInfo);
		
		return;
	}
	
	private static void processREQUEST_DIST_Video_PROC(CMVideoEvent fe, CMInfo cmInfo)
	{
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMVideoTransferManager.processREQUEST_DIST_Video_PROC(), user("
						+fe.getUserName()+") requests the distributed Video processing.");
		}
		return;
	}
	
	private static void processREQUEST_VIDEO_STREAMING_CHAN(CMVideoEvent fe, CMInfo cmInfo)
	{
		CMStreamingInfo fInfo = cmInfo.getStreamingInfo();
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMVideoTransferManager.processREQUEST_VIDEO_STREAMING_CHAN(), requester("
					+fe.getUserName()+"), Video("+fe.getVideoName()+"), contentID("+fe.getContentID()
					+").");
		}

		String strVideoName = fe.getVideoName();
		CMVideoEvent feAck = new CMVideoEvent();
		feAck.setID(CMVideoEvent.REPLY_VIDEO_STREAMING_CHAN);
		feAck.setVideoName(strVideoName);

		// get the full path of the requested Video
		String strFullPath = fInfo.getVideoPath() + File.separator + strVideoName; 
		// check the Video existence
		File Video = new File(strFullPath);
		System.out.println("- Full Path : " + strFullPath);
		if(!Video.exists())
		{
			feAck.setReturnCode(0);	// Video not found
			CMEventManager.unicastEvent(feAck, fe.getUserName(), cmInfo);
			feAck = null;
			return;
		}
		
		feAck.setReturnCode(1);	// Video found
		feAck.setContentID(fe.getContentID());
		CMEventManager.unicastEvent(feAck, fe.getUserName(), cmInfo);
		
		pushVideoWithSepChannel(strFullPath, fe.getUserName(), fe.getContentID(), cmInfo);
		return;
	}
	
	private static void processREPLY_VIDEO_STREAMING_CHAN(CMVideoEvent fe, CMInfo cmInfo)
	{
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMVideoManager.processREPLY_VIDEO_STREAMING_CHAN(), Video("+fe.getVideoName()
					+"), return code("+fe.getReturnCode()+"), contentID("+fe.getContentID()+").");
		}
		return;
	}
	
	private static void processSTART_VIDEO_STREAMING_CHAN(CMVideoEvent fe, CMInfo cmInfo)
	{
		CMStreamingInfo fInfo = cmInfo.getStreamingInfo();
		CMConfigurationInfo confInfo = cmInfo.getConfigurationInfo();
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMVideoTransferManager.processSTART_VIDEO_STREAMING_CHAN(),");
			System.out.println("sender("+fe.getSenderName()+"), Video("+fe.getVideoName()+"), size("
					+fe.getVideoSize()+"), contentID("+fe.getContentID()+").");
		}
		
		// set Video size
		long lVideoSize = fe.getVideoSize();
		
		// set a path of the received Video
		String strFullPath = fInfo.getVideoPath();
		if(confInfo.getSystemType().equals("CLIENT"))
		{
			strFullPath = strFullPath + File.separator + fe.getVideoName();
		}
		else if(confInfo.getSystemType().equals("SERVER"))
		{
			// check the sub-directory and create it if it does not exist
			strFullPath = strFullPath + File.separator + fe.getSenderName();
			File subDir = new File(strFullPath);
			if(!subDir.exists() || !subDir.isDirectory())
			{
				boolean ret = subDir.mkdirs();
				if(ret)
				{
					if(CMInfo._CM_DEBUG)
						System.out.println("A sub-directory is created.");
				}
				else
				{
					System.err.println("A sub-directory cannot be created!");
					return;
				}
			}
			
			strFullPath = strFullPath + File.separator + fe.getVideoName();
		}
		else
		{
			System.err.println("Wrong system type!");
			return;
		}		
		
		// get the default blocking TCP socket channel
		SocketChannel sc = null;
		SocketChannel dsc = null;
		if(confInfo.getSystemType().equals("CLIENT"))	// CLIENT
		{
			CMServer serverInfo = cmInfo.getInteractionInfo().getDefaultServerInfo();
			sc = (SocketChannel) serverInfo.getBlockSocketChannelInfo().findChannel(0);
			dsc = (SocketChannel) serverInfo.getNonBlockSocketChannelInfo().findChannel(0);
		}
		else	// SERVER
		{
			CMUser user = cmInfo.getInteractionInfo().getLoginUsers().findMember(fe.getSenderName());
			sc = (SocketChannel) user.getBlockSocketChannelInfo().findChannel(0);
			dsc = (SocketChannel) user.getNonBlockSocketChannelInfo().findChannel(0);
		}
		
		if(sc == null)
		{
			System.err.println("CMVideoTransferManager.processSTART_VIDEO_STREAMING_CHAN();"
					+ "the default blocking TCP socket channel not found!");
			return;
		}
		else if(!sc.isOpen())
		{
			System.err.println("CMVideoTransferManager.processSTART_VIDEO_STREAMING_CHAN();"
					+ "the default blocking TCP socket channel is closed!");
			return;
		}
		
		if(dsc == null)
		{
			System.err.println("CMVideoTransferManager.processSTART_VIDEO_STREAMING_CHAN();"
					+ "the default TCP socket channel not found!");
			return;
		}
		else if(!dsc.isOpen())
		{
			System.err.println("CMVideoTransferManager.processSTART_VIDEO_STREAMING_CHAN();"
					+ "the default TCP socket channel is closed!");
			return;
		}

		// check the existing Video
		File Video = new File(strFullPath);
		long lRecvSize = 0;
		if(Video.exists())
		{
			// init received Video size
			lRecvSize = Video.length();
		}

		// add the received Video info
		boolean bResult = false;
		CMRecvVideoInfo rfInfo = new CMRecvVideoInfo();
		rfInfo.setSenderName(fe.getSenderName());
		rfInfo.setReceiverName(cmInfo.getInteractionInfo().getMyself().getName());
		rfInfo.setVideoName(fe.getVideoName());
		rfInfo.setVideoPath(strFullPath);
		rfInfo.setVideoSize(lVideoSize);
		rfInfo.setContentID(fe.getContentID());
		rfInfo.setRecvSize(lRecvSize);
		//rfInfo.setWriteVideo(raf);
		rfInfo.setRecvChannel(sc);
		rfInfo.setDefaultChannel(dsc);
		
		bResult = fInfo.addRecvVideoInfo(rfInfo);
		if(!bResult)
		{
			System.err.println("CMVideoTransferManager.processSTART_VIDEO_STREAMING_CHAN(); failed to add "
					+ "the receiving Video info!");
			return;
		}
		
		if(!fInfo.isRecvOngoing(fe.getSenderName()))
		{
			sendSTART_VIDEO_STREAMING_CHAN_ACK(rfInfo, cmInfo);
			/*
			// start a dedicated thread to receive the Video
			Future<CMRecvVideoInfo> future = null;
			CMRecvVideoTask recvVideoTask = new CMRecvVideoTask(rfInfo);
			future = fInfo.getExecutorService().submit(recvVideoTask, rfInfo);
			rfInfo.setRecvTaskResult(future);
			
			// send ack event
			CMVideoEvent feAck = new CMVideoEvent();
			feAck.setID(CMVideoEvent.START_VIDEO_STREAMING_CHAN_ACK);
			feAck.setUserName(cmInfo.getInteractionInfo().getMyself().getName());
			feAck.setVideoName(fe.getVideoName());
			feAck.setContentID(fe.getContentID());
			feAck.setReceivedVideoSize(lRecvSize);
			CMEventManager.unicastEvent(feAck, fe.getSenderName(), cmInfo);

			feAck = null;
			*/
		}
				
		return;
	}
	
	private static void processSTART_VIDEO_STREAMING_CHAN_ACK(CMVideoEvent fe, CMInfo cmInfo)
	{
		String strReceiver = null;
		String strVideoName = null;
		String strFullVideoName = null;
		long lVideoSize = -1;	// Video size
		int nContentID = -1;
		long lRecvSize = -1;	// received size by the receiver
		CMStreamingInfo fInfo = cmInfo.getStreamingInfo();
		CMSendVideoInfo sInfo = null;
		
		// find the CMSendVideoInfo object 
		sInfo = fInfo.findSendVideoInfo(fe.getUserName(), fe.getVideoName(), fe.getContentID());
		if(sInfo == null)
		{
			System.err.println("CMVideoTransferManager.processSTART_VIDEO_STREAMING_CHAN_ACK(), sendVideoInfo "
					+ "not found! : receiver("+fe.getUserName()+"), Video("+fe.getVideoName()
					+"), content ID("+fe.getContentID()+")");
			return;
		}
		
		strReceiver = sInfo.getReceiverName();
		strFullVideoName = sInfo.getVideoPath();
		strVideoName = getVideoNameFromPath(strFullVideoName);
		lVideoSize = sInfo.getVideoSize();
		nContentID = sInfo.getContentID();
		
		lRecvSize = fe.getReceivedVideoSize();
		if(lRecvSize > 0)
			sInfo.setSentSize(lRecvSize);	// update the sent size 
					
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMVideoTransferManager.processSTART_VIDEO_STREAMING_CHAN_ACK(); "
					+ "receiver("+strReceiver+"), Video path("+strFullVideoName+"), Video name("+strVideoName
					+ "), Video size("+lVideoSize+"), content ID("+nContentID+").");
			System.out.println("already received Video size by the receiver("+lRecvSize+").");
		}
		
		CMCommInfo commInfo = cmInfo.getCommInfo();
		// start a dedicated sending thread
		Future<CMSendVideoInfo> future = null;
		CMSendVideoTask sendVideoTask = new CMSendVideoTask(sInfo, commInfo.getSendBlockingEventQueue());
		future = fInfo.getExecutorService().submit(sendVideoTask, sInfo);
		sInfo.setSendTaskResult(future);		

		return;		
	}
	
	private static void processEND_VIDEO_STREAMING_CHAN(CMVideoEvent fe, CMInfo cmInfo)
	{
		CMStreamingInfo fInfo = cmInfo.getStreamingInfo();
		CMInteractionInfo interInfo = cmInfo.getInteractionInfo();
		boolean bResult = false;

		// find info from recv Video list
		CMRecvVideoInfo recvInfo = fInfo.findRecvVideoInfo(fe.getSenderName(), fe.getVideoName(), fe.getContentID());
		if(recvInfo == null)
		{
			System.err.println("CMVideoTransferManager.processEND_VIDEO_STREAMING_CHAN(), recv Video info "
					+"for sender("+fe.getSenderName()+"), Video("+fe.getVideoName()+"), content ID("
					+fe.getContentID()+") not found.");
			return;
		}

		// wait the receiving thread
		if(!recvInfo.getRecvTaskResult().isDone())
		{
			try {
				recvInfo.getRecvTaskResult().get();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMVideoTransferManager.processEND_VIDEO_STREAMING_CHAN(), sender("+fe.getSenderName()
					+"), Video("+fe.getVideoName()+"), contentID("+fe.getContentID()+"), Video size("
					+recvInfo.getVideoSize()+"), received size("+recvInfo.getRecvSize()+").");
		}

		// make ack event
		CMVideoEvent feAck = new CMVideoEvent();
		feAck.setID(CMVideoEvent.END_VIDEO_STREAMING_CHAN_ACK);
		feAck.setUserName(interInfo.getMyself().getName());
		feAck.setVideoName(fe.getVideoName());
		feAck.setContentID(fe.getContentID());

		// check out whether the Video is completely received
		if(recvInfo.getVideoSize() == recvInfo.getRecvSize())
		{
			feAck.setReturnCode(1);	// success
			bResult = true;
		}
		else
		{
			System.err.println("CMVideoTransferManager.processEND_VIDEO_STREAMING_CHAN(); incompletely received!");
			feAck.setReturnCode(0); // failure
			bResult = false;
		}
		
		// remove info from push Video list
		fInfo.removeRecvVideoInfo(fe.getSenderName(), fe.getVideoName(), fe.getContentID());
		
		// send ack
		CMEventManager.unicastEvent(feAck, fe.getSenderName(), cmInfo);		
		feAck = null;

		//if(bResult)
		//	CMSNSManager.checkCompleteRecvAttachedVideos(fe, cmInfo);

		// check whether there is a remaining receiving Video info or not
		CMRecvVideoInfo nextRecvInfo = fInfo.findRecvVideoInfoNotStarted(fe.getSenderName());
		if(nextRecvInfo != null)
		{
			sendSTART_VIDEO_STREAMING_CHAN_ACK(nextRecvInfo, cmInfo);
		}
		
		return;
	}
	
	private static void processEND_VIDEO_STREAMING_CHAN_ACK(CMVideoEvent fe, CMInfo cmInfo)
	{
		CMStreamingInfo fInfo = cmInfo.getStreamingInfo();
		String strReceiverName = fe.getUserName();
		String strVideoName = fe.getVideoName();
		int nContentID = fe.getContentID();
		
		// find completed send info
		CMSendVideoInfo sInfo = fInfo.findSendVideoInfo(strReceiverName, strVideoName, nContentID);
		if(sInfo == null)
		{
			System.err.println("CMVideoTransferManager.processEND_VIDEO_STREAMING_CHAN_ACK(), send info not found");
			System.err.println("receiver("+strReceiverName+"), Video("+strVideoName+"), content ID("+nContentID+").");
		}
		else
		{
			// delete corresponding request from the list
			fInfo.removeSendVideoInfo(strReceiverName, strVideoName, nContentID);
		}
	
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMVideoTransferManager.processEND_VIDEO_STREAMING_CHAN_ACK(), receiver("
					+strReceiverName+"), Video("+strVideoName+"), return code("+fe.getReturnCode()
					+"), contentID("+nContentID+").");
		}
		
		//////////////////// check the completion of sending attached Video of SNS content
		//////////////////// and check the completion of prefetching an attached Video of SNS content
		//CMSNSManager.checkCompleteSendAttachedVideos(fe, cmInfo);

		return;	
	}
	
	private static void sendSTART_VIDEO_STREAMING_CHAN_ACK(CMRecvVideoInfo rfInfo, CMInfo cmInfo)
	{
		CMStreamingInfo fInfo = cmInfo.getStreamingInfo();

		// start a dedicated thread to receive the Video
		Future<CMRecvVideoInfo> future = null;
		CMRecvVideoTask recvVideoTask = new CMRecvVideoTask(rfInfo);
		future = fInfo.getExecutorService().submit(recvVideoTask, rfInfo);
		rfInfo.setRecvTaskResult(future);
		
		// send ack event
		CMVideoEvent feAck = new CMVideoEvent();
		feAck.setID(CMVideoEvent.START_VIDEO_STREAMING_CHAN_ACK);
		feAck.setUserName(cmInfo.getInteractionInfo().getMyself().getName());
		feAck.setVideoName(rfInfo.getVideoName());
		feAck.setContentID(rfInfo.getContentID());
		feAck.setReceivedVideoSize(rfInfo.getRecvSize());
		CMEventManager.unicastEvent(feAck, rfInfo.getSenderName(), cmInfo);

		feAck = null;
	}
	
}
