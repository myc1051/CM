package kr.ac.konkuk.ccslab.cm.thread;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.RandomAccessFile;
import java.io.StringWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.UUID;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.JLabel;
import javax.swing.Timer;


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
	
    //RTP variables:
    //----------------
    DatagramSocket RTPsocket; //socket to be used to send and receive UDP packets
    DatagramPacket senddp; //UDP packet containing the video frames

    InetAddress ClientIPAddr;   //Client IP address
    int RTP_dest_port = 0;      //destination port for RTP packets  (given by the RTSP Client)
    int RTSP_dest_port = 0;

    //GUI:
    //----------------
    JLabel label;

    //Video variables:
    //----------------
    int imagenb = 0; //image nb of the image currently transmitted
    VideoStream video; //VideoStream object used to access video frames
    static int MJPEG_TYPE = 26; //RTP payload type for MJPEG video
    static int FRAME_PERIOD = 50; //Frame period of the video to stream, in ms
    static int VIDEO_LENGTH = 500; //length of the video in frames

    Timer timer;    //timer used to send the images at the video frame rate
    byte[] buf;     //buffer used to store the images to send to the client 
    int sendDelay;  //the delay to send images over the wire. Ideally should be
                    //equal to the frame rate of the video file, but may be 
                    //adjusted when congestion is detected.

    //RTSP variables
    //----------------
    //rtsp states
    final static int INIT = 0;
    final static int READY = 1;
    final static int PLAYING = 2;
    //rtsp message types
    final static int SETUP = 3;
    final static int PLAY = 4;
    final static int PAUSE = 5;
    final static int TEARDOWN = 6;
    final static int DESCRIBE = 7;

    static int state; //RTSP Server state == INIT or READY or PLAY
    Socket RTSPsocket; //socket used to send/receive RTSP messages
    //input and output stream filters
    static BufferedReader RTSPBufferedReader;
    static BufferedWriter RTSPBufferedWriter;
    static String VideoFileName; //video file requested from the client
    static String RTSPid = UUID.randomUUID().toString(); //ID of the RTSP session
    int RTSPSeqNb = 0; //Sequence number of RTSP messages within the session


    //RTCP variables
    //----------------
    static int RTCP_RCV_PORT = 19001; //port where the client will receive the RTP packets
    static int RTCP_PERIOD = 400;     //How often to check for control events
    DatagramSocket RTCPsocket;
    RtcpReceiver rtcpReceiver;
    int congestionLevel;

    //Performance optimization and Congestion control
    ImageTranslator imgTranslator;
    CongestionController cc;
    
    final static String CRLF = "\r\n";

  
    
	public CMSendVideoTask(CMSendVideoInfo sendVideoInfo, CMBlockingEventQueue sendQueue)
	{
		m_sendVideoInfo = sendVideoInfo;
		m_sendQueue = sendQueue;
		
        //init congestion controller
        cc = new CongestionController(600);

        //allocate memory for the sending buffer
        buf = new byte[20000]; 

        //init the RTCP packet receiver
        rtcpReceiver = new RtcpReceiver(RTCP_PERIOD);
        

	}
	
	@Override
	public void run() {
		RandomAccessFile raf = null;
		FileChannel fc = null;
		long lSentSize = m_sendVideoInfo.getSentSize();
		long lVideoSize = m_sendVideoInfo.getVideoSize();
		//SocketChannel sendSC = m_sendVideoInfo.getSendChannel();
		DatagramChannel sendDC = m_sendVideoInfo.getSendChannel();
		int nReadBytes = -1;
		int nSendBytes = -1;
		int nSendBytesSum = -1;
		//ByteBuffer buf = ByteBuffer.allocateDirect(CMInfo.VIDEO_BLOCK_LEN);
		CMVideoEvent fe = null;
		boolean bInterrupted = false;
        ByteBuffer send_buf = ByteBuffer.allocateDirect(CMInfo.VIDEO_BLOCK_LEN);

		byte[] frame;
		int imagenb = 0;
		int congestionLevel = 0;
		
		VideoFileName = "1.Mjpeg";
		
        //init the VideoStream object:
		try {
			video = new VideoStream(VideoFileName);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

               
		//Performance optimization and Congestion control
	    ImageTranslator imgTranslator;
	    CongestionController cc;
	    
	    //Video encoding and quality
        imgTranslator = new ImageTranslator(0.8f);
        System.out.println("비디오 길이 : " + lVideoSize);
		//if the current image nb is less than the length of the video
        while (imagenb < 500) {
            //update current imagenb
            imagenb++;
            System.out.println("전송할 이미지 번호 : " + imagenb + " / " +lVideoSize) ;
           
            try {
                //get next frame to send from the video, as well as its size
                int image_length = video.getnextframe(buf);
                System.out.println("이미지 크기 : " + image_length);
                
                //adjust quality of the image if there is congestion detected
                if (congestionLevel > 0) {	
                    imgTranslator.setCompressionQuality(1.0f - congestionLevel * 0.2f);
                    frame = imgTranslator.compress(Arrays.copyOfRange(buf, 0, image_length));
                    image_length = frame.length;
                    System.arraycopy(frame, 0, buf, 0, image_length);
                }
                
                //Builds an RTPpacket object containing the frame
                RTPpacket rtp_packet = new RTPpacket(MJPEG_TYPE, imagenb, imagenb*FRAME_PERIOD, buf, image_length);
                
                //get to total length of the full rtp packet to send
                int packet_length = rtp_packet.getlength();

                //retrieve the packet bitstream and store it in an array of bytes
                byte[] packet_bits = new byte[packet_length];
                rtp_packet.getpacket(packet_bits);
                senddp = new DatagramPacket(packet_bits, packet_length, ClientIPAddr, RTP_dest_port);
                
                System.out.println("데이터그램 패킷 크기 : " + senddp.getLength());
                rtp_packet.printheader();
                
                send_buf.mark();
                send_buf.put(senddp.getData());
                send_buf.reset();
                sendDC.send(send_buf, new InetSocketAddress("localhost", 60000));
                //sendDC.write(send_buf);
                System.out.println(imagenb + "번 이미지 전송 완료");
                
                send_buf.clear();
                
                Thread.sleep(20);
                /*
                
                //send the packet as a DatagramPacket over the UDP socket 
                senddp = new DatagramPacket(packet_bits, packet_length, ClientIPAddr, RTP_dest_port);
                RTPsocket.send(senddp);

                System.out.println("Send frame #" + imagenb + ", Frame size: " + image_length + " (" + buf.length + ")");
                //print the header bitstream
                rtp_packet.printheader();
                //update GUI
                label.setText("Send frame #" + imagenb);
                */
                
            }
            catch(Exception ex) {
                System.out.println("Exception caught: "+ex);
                System.exit(0);
            }
        }
        timer.stop();
        rtcpReceiver.stopRcv();
        
/*
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
*/
		
		
		
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

    //------------------------
    //Controls RTP sending rate based on traffic
    //------------------------
    class CongestionController implements ActionListener {
        private Timer ccTimer;
        int interval;   //interval to check traffic stats
        int prevLevel;  //previously sampled congestion level

        public CongestionController(int interval) {
            this.interval = interval;
            ccTimer = new Timer(interval, this);
            ccTimer.start();
        }

        public void actionPerformed(ActionEvent e) {

            //adjust the send rate
            if (prevLevel != congestionLevel) {
                sendDelay = FRAME_PERIOD + congestionLevel * (int)(FRAME_PERIOD * 0.1);
                timer.setDelay(sendDelay);
                prevLevel = congestionLevel;
                System.out.println("Send delay changed to: " + sendDelay);
            }
        }
    }

    //------------------------
    //Listener for RTCP packets sent from client
    //------------------------
    class RtcpReceiver implements ActionListener {
        private Timer rtcpTimer;
        private byte[] rtcpBuf;
        int interval;

        public RtcpReceiver(int interval) {
            //set timer with interval for receiving packets
            this.interval = interval;
            rtcpTimer = new Timer(interval, this);
            rtcpTimer.setInitialDelay(0);
            rtcpTimer.setCoalesce(true);

            //allocate buffer for receiving RTCP packets
            rtcpBuf = new byte[512];
        }

        public void actionPerformed(ActionEvent e) {
            //Construct a DatagramPacket to receive data from the UDP socket
            DatagramPacket dp = new DatagramPacket(rtcpBuf, rtcpBuf.length);
            float fractionLost;

            try {
                RTCPsocket.receive(dp);   // Blocking
                RTCPpacket rtcpPkt = new RTCPpacket(dp.getData(), dp.getLength());
                System.out.println("[RTCP] " + rtcpPkt);

                //set congestion level between 0 to 4
                fractionLost = rtcpPkt.fractionLost;
                if (fractionLost >= 0 && fractionLost <= 0.01) {
                    congestionLevel = 0;    //less than 0.01 assume negligible
                }
                else if (fractionLost > 0.01 && fractionLost <= 0.25) {
                    congestionLevel = 1;
                }
                else if (fractionLost > 0.25 && fractionLost <= 0.5) {
                    congestionLevel = 2;
                }
                else if (fractionLost > 0.5 && fractionLost <= 0.75) {
                    congestionLevel = 3;
                }
                else {
                    congestionLevel = 4;
                }
            }
            catch (InterruptedIOException iioe) {
                System.out.println("Nothing to read");
            }
            catch (IOException ioe) {
                System.out.println("Exception caught: "+ioe);
            }
        }

        public void startRcv() {
            rtcpTimer.start();
        }

        public void stopRcv() {
            rtcpTimer.stop();
        }
    }

    //------------------------------------
    //Translate an image to different encoding or quality
    //------------------------------------
    class ImageTranslator {

        private float compressionQuality;
        private ByteArrayOutputStream baos;
        private BufferedImage image;
        private Iterator<ImageWriter>writers;
        private ImageWriter writer;
        private ImageWriteParam param;
        private ImageOutputStream ios;

        public ImageTranslator(float cq) {
            compressionQuality = cq;

            try {
                baos =  new ByteArrayOutputStream();
                ios = ImageIO.createImageOutputStream(baos);

                writers = ImageIO.getImageWritersByFormatName("jpeg");
                writer = (ImageWriter)writers.next();
                writer.setOutput(ios);

                param = writer.getDefaultWriteParam();
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(compressionQuality);

            } catch (Exception ex) {
                System.out.println("Exception caught: "+ex);
                System.exit(0);
            }
        }

        public byte[] compress(byte[] imageBytes) {
            try {
                baos.reset();
                image = ImageIO.read(new ByteArrayInputStream(imageBytes));
                writer.write(null, new IIOImage(image, null, null), param);
            } catch (Exception ex) {
                System.out.println("Exception caught: "+ex);
                System.exit(0);
            }
            return baos.toByteArray();
        }

        public void setCompressionQuality(float cq) {
            compressionQuality = cq;
            param.setCompressionQuality(compressionQuality);
        }
    }

    //------------------------------------
    //Parse RTSP Request
    //------------------------------------
    private int parseRequest() {
        int request_type = -1;
        try { 
            //parse request line and extract the request_type:
            String RequestLine = RTSPBufferedReader.readLine();
            System.out.println("RTSP Server - Received from Client:");
            System.out.println(RequestLine);

            StringTokenizer tokens = new StringTokenizer(RequestLine);
            String request_type_string = tokens.nextToken();

            //convert to request_type structure:
            if ((new String(request_type_string)).compareTo("SETUP") == 0)
                request_type = SETUP;
            else if ((new String(request_type_string)).compareTo("PLAY") == 0)
                request_type = PLAY;
            else if ((new String(request_type_string)).compareTo("PAUSE") == 0)
                request_type = PAUSE;
            else if ((new String(request_type_string)).compareTo("TEARDOWN") == 0)
                request_type = TEARDOWN;
            else if ((new String(request_type_string)).compareTo("DESCRIBE") == 0)
                request_type = DESCRIBE;

            if (request_type == SETUP) {
                //extract VideoFileName from RequestLine
                VideoFileName = tokens.nextToken();
            }

            //parse the SeqNumLine and extract CSeq field
            String SeqNumLine = RTSPBufferedReader.readLine();
            System.out.println(SeqNumLine);
            tokens = new StringTokenizer(SeqNumLine);
            tokens.nextToken();
            RTSPSeqNb = Integer.parseInt(tokens.nextToken());
        
            //get LastLine
            String LastLine = RTSPBufferedReader.readLine();
            System.out.println(LastLine);

            tokens = new StringTokenizer(LastLine);
            if (request_type == SETUP) {
                //extract RTP_dest_port from LastLine
                for (int i=0; i<3; i++)
                    tokens.nextToken(); //skip unused stuff
                RTP_dest_port = Integer.parseInt(tokens.nextToken());
            }
            else if (request_type == DESCRIBE) {
                tokens.nextToken();
                String describeDataType = tokens.nextToken();
            }
            else {
                //otherwise LastLine will be the SessionId line
                tokens.nextToken(); //skip Session:
                RTSPid = tokens.nextToken();
            }
        } catch(Exception ex) {
            System.out.println("Exception caught: "+ex);
            System.exit(0);
        }
      
        return(request_type);
    }

    // Creates a DESCRIBE response string in SDP format for current media
    private String describe() {
        StringWriter writer1 = new StringWriter();
        StringWriter writer2 = new StringWriter();
        
        // Write the body first so we can get the size later
        writer2.write("v=0" + CRLF);
        writer2.write("m=video " + RTSP_dest_port + " RTP/AVP " + MJPEG_TYPE + CRLF);
        writer2.write("a=control:streamid=" + RTSPid + CRLF);
        writer2.write("a=mimetype:string;\"video/MJPEG\"" + CRLF);
        String body = writer2.toString();

        writer1.write("Content-Base: " + VideoFileName + CRLF);
        writer1.write("Content-Type: " + "application/sdp" + CRLF);
        writer1.write("Content-Length: " + body.length() + CRLF);
        writer1.write(body);
        
        return writer1.toString();
    }

    //------------------------------------
    //Send RTSP Response
    //------------------------------------
    private void sendResponse() {
        try {
            RTSPBufferedWriter.write("RTSP/1.0 200 OK"+CRLF);
            RTSPBufferedWriter.write("CSeq: "+RTSPSeqNb+CRLF);
            RTSPBufferedWriter.write("Session: "+RTSPid+CRLF);
            RTSPBufferedWriter.flush();
            System.out.println("RTSP Server - Sent response to Client.");
        } catch(Exception ex) {
            System.out.println("Exception caught: "+ex);
            System.exit(0);
        }
    }

    private void sendDescribe() {
        String des = describe();
        try {
            RTSPBufferedWriter.write("RTSP/1.0 200 OK"+CRLF);
            RTSPBufferedWriter.write("CSeq: "+RTSPSeqNb+CRLF);
            RTSPBufferedWriter.write(des);
            RTSPBufferedWriter.flush();
            System.out.println("RTSP Server - Sent response to Client.");
        } catch(Exception ex) {
            System.out.println("Exception caught: "+ex);
            System.exit(0);
        }
    }

}
