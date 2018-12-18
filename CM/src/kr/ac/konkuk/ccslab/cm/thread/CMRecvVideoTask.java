package kr.ac.konkuk.ccslab.cm.thread;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;

import kr.ac.konkuk.ccslab.cm.entity.CMRecvVideoInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;

public class CMRecvVideoTask implements Runnable {

	CMRecvVideoInfo m_recvVideoInfo;
	
	public CMRecvVideoTask(CMRecvVideoInfo recvVideoInfo)
	{
		m_recvVideoInfo = recvVideoInfo;
	}
	
	@Override
	public void run() {
		
		RandomAccessFile raf = null;
		FileChannel fc = null;
		long lRecvSize = m_recvVideoInfo.getRecvSize();
		long lVideoSize = m_recvVideoInfo.getVideoSize();
		SocketChannel recvSC = m_recvVideoInfo.getRecvChannel();
		int nRecvBytes = -1;
		int nWrittenBytes = -1;
		int nWrittenBytesSum = -1;
		ByteBuffer buf = ByteBuffer.allocateDirect(CMInfo.VIDEO_BLOCK_LEN);
		boolean bInterrupted = false;
		
		// open the Video
		try {
			raf = new RandomAccessFile(m_recvVideoInfo.getVideoPath(), "rw");
			fc = raf.getChannel();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		
		// skip Video offset by the previously received size
		if(lRecvSize > 0)
		{
			try {
				//raf.seek(lRecvSize);
				fc.position(lRecvSize);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				closeRandomAccessVideo(raf);				
				return;
			}
		}
		
		// main loop for receiving and writing Video blocks
		nRecvBytes = 0;
		while(lRecvSize < lVideoSize && !bInterrupted)
		{
			// check for interrupt by other thread
			if(Thread.currentThread().isInterrupted())
			{
				if(CMInfo._CM_DEBUG)
				{
					System.out.println("CMRecvVideoTask.run(); interrupted at the outer loop! Video name("
							+m_recvVideoInfo.getVideoName()+"), Video size("+lVideoSize+"), recv size("+lRecvSize+").");
				}
				bInterrupted = true;
				continue;
			}
			
			// initialize the ByteBuffer
			buf.clear();
			
			// receive a Video block
			try {
				nRecvBytes = recvSC.read(buf);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				closeRandomAccessVideo(raf);
				return;
			}
			
			// write the received block to the Video
			buf.flip();
			nWrittenBytesSum = 0;
			while(nWrittenBytesSum < nRecvBytes && !bInterrupted)
			{
				if(Thread.currentThread().isInterrupted())
				{
					if(CMInfo._CM_DEBUG)
					{
						System.out.println("CMRecvVideoTask.run(); interrupted at the inner loop! Video name("
								+m_recvVideoInfo.getVideoName()+"), Video size("+lVideoSize+"), recv size("+lRecvSize+").");						
					}
					bInterrupted = true;
					continue;
				}
				
				try {
					nWrittenBytes = fc.write(buf);
					
					// update the size of received and written Video blocks
					lRecvSize += nWrittenBytes;
					m_recvVideoInfo.setRecvSize(lRecvSize);
					nWrittenBytesSum += nWrittenBytes;
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					closeRandomAccessVideo(raf);
					return;
				}	
			} // inner while loop
			
		} // outer while loop
		
		closeRandomAccessVideo(raf);
		
		return;

	}
	
	private void closeRandomAccessVideo(RandomAccessFile raf)
	{
		try {
			raf.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
