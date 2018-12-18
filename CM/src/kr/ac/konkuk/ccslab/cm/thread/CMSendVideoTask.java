package kr.ac.konkuk.ccslab.cm.thread;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;

import kr.ac.konkuk.ccslab.cm.entity.CMMessage;
import kr.ac.konkuk.ccslab.cm.entity.CMSendVideoInfo;
import kr.ac.konkuk.ccslab.cm.event.CMBlockingEventQueue;
import kr.ac.konkuk.ccslab.cm.event.CMVideoEvent;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
//import kr.ac.konkuk.ccslab.cm.manager.CMCommManager;
import kr.ac.konkuk.ccslab.cm.manager.CMEventManager;

public class CMSendVideoTask implements Runnable {

	CMSendVideoInfo m_sendVideoInfo;
	CMBlockingEventQueue m_sendQueue;
	
	public CMSendVideoTask(CMSendVideoInfo sendVideoInfo, CMBlockingEventQueue sendQueue)
	{
		m_sendVideoInfo = sendVideoInfo;
		m_sendQueue = sendQueue;
	}
	
	@Override
	public void run() {
		RandomAccessFile raf = null;
		FileChannel fc = null;
		long lSentSize = m_sendVideoInfo.getSentSize();
		long lVideoSize = m_sendVideoInfo.getVideoSize();
		SocketChannel sendSC = m_sendVideoInfo.getSendChannel();
		int nReadBytes = -1;
		int nSendBytes = -1;
		int nSendBytesSum = -1;
		ByteBuffer buf = ByteBuffer.allocateDirect(CMInfo.VIDEO_BLOCK_LEN);
		CMVideoEvent fe = null;
		boolean bInterrupted = false;

		// open the Video
		try {
			raf = new RandomAccessFile(m_sendVideoInfo.getVideoPath(), "rw");
			fc = raf.getChannel();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		
		// skip Video offset by the previously sent size
		if(lSentSize > 0)
		{
			try {
				//raf.seek(lRecvSize);
				fc.position(lSentSize);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				closeRandomAccessFile(raf);				
				return;
			}
		}

		// main loop for receiving and writing Video blocks
		nSendBytes = 0;
		while( lSentSize < lVideoSize && !bInterrupted)
		{
			// check for interrupt by other thread
			if(Thread.currentThread().isInterrupted())
			{
				if(CMInfo._CM_DEBUG)
				{
					System.out.println("CMSendVideoTask.run(); interrupted at the outer loop! Video name("
							+m_sendVideoInfo.getVideoName()+"), Video size("+lVideoSize+"), sent size("+lSentSize+").");
				}

				bInterrupted = true;
				continue;
			}
			
			// initialize the ByteBuffer
			buf.clear();
			
			// read a Video block
			try {
				nReadBytes = fc.read(buf);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				closeRandomAccessFile(raf);
				return;
			}
			
			// send a Video block
			buf.flip();
			nSendBytesSum = 0;
			while(nSendBytesSum < nReadBytes && !bInterrupted)
			{
				if(Thread.currentThread().isInterrupted())
				{
					if(CMInfo._CM_DEBUG)
					{
						System.out.println("CMSendVideoTask.run(); interrupted at the inner loop! Video name("
								+m_sendVideoInfo.getVideoName()+"), Video size("+lVideoSize+"), sent size("+lSentSize+").");
					}
					bInterrupted = true;
					continue;
				}
				
				try {
					nSendBytes = sendSC.write(buf);
					
					// update the size of read and sent Video blocks
					lSentSize += nSendBytes;
					m_sendVideoInfo.setSentSize(lSentSize);
					nSendBytesSum += nSendBytes;
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					closeRandomAccessFile(raf);
					return;
				}
			} // inner while loop
			
		} // outer while loop

		//if(!bInterrupted)
		if(lSentSize >= lVideoSize)
		{
			if(lSentSize > lVideoSize)
			{
				System.err.println("CMSendVideoTask.run(); the receiver("+m_sendVideoInfo.getReceiverName()+") already has "
						+ "a bigger size Video("+m_sendVideoInfo.getVideoName()+"); sender size("+lVideoSize
						+ "), receiver size("+lSentSize+")");
			}
			
			// send END_Video_TRANSFER_CHAN with the default TCP socket channel
			fe = new CMVideoEvent();
			fe.setID(CMVideoEvent.END_VIDEO_STREAMING_CHAN);
			fe.setSenderName(m_sendVideoInfo.getSenderName());
			fe.setVideoName(m_sendVideoInfo.getVideoName());
			fe.setVideoSize(m_sendVideoInfo.getVideoSize());
			fe.setContentID(m_sendVideoInfo.getContentID());
			
			CMMessage msg = new CMMessage(CMEventManager.marshallEvent(fe), m_sendVideoInfo.getDefaultChannel());
			m_sendQueue.push(msg);
			//CMCommManager.sendMessage(CMEventManager.marshallEvent(fe), m_sendVideoInfo.getDefaultChannel());
			//fe = null;
		}

		closeRandomAccessFile(raf);
		
		return;
	}
	
	private void closeRandomAccessFile(RandomAccessFile raf)
	{
		try {
			raf.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
