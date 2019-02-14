import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Class that implements the channel used by wizards and miners to communicate.
 */
public class CommunicationChannel {
	private static String NO_PARENT = "NO_PARENT";
    public static String END = "END";
    public static String EXIT = "EXIT";
    
    //dimensiunea buffer-elor pentru cele 2 canale
    int max = 1000;
    //retine nodurile trimise spre Miners pentru procesare
    Set<Integer> putNodes = new HashSet<>(max);
    //retine thread-urile care au trimis nodul parinte
	Map<Long,Message> map = new HashMap<Long,Message>();
	
	//retine thread-ul care va trimite mesajele de EXIT
	long finishThread = -1;
    
	//buffer pentru Wizards channel

	ArrayBlockingQueue<Message> bufferWizardChannel;

	//buffer pentru Miners channel
	
	ArrayBlockingQueue<Message> bufferMinerChannel;
	

	/**
	 * Creates a {@code CommunicationChannel} object.
	 */
	
	public CommunicationChannel() {
		bufferWizardChannel = new ArrayBlockingQueue<Message>(max);
	
		bufferMinerChannel = new ArrayBlockingQueue<Message>(max);
	}

	/**
	 * Puts a message on the miner channel (i.e., where miners write to and wizards
	 * read from).
	 * 
	 * @param message
	 *            message to be put on the channel
	 */
	public void putMessageMinerChannel(Message message) {
		
		try {
			//se pune mmesajul in buffer
			bufferMinerChannel.put(message);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		
	}

	/**
	 * Gets a message from the miner channel (i.e., where miners write to and
	 * wizards read from).
	 * 
	 * @return message from the miner channel
	 */
	public Message getMessageMinerChannel() {
		
		Message temp = null;	
		try {
			//se ia mesajul din buffer
			temp = bufferMinerChannel.take();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return temp;
	}

	/**
	 * Puts a message on the wizard channel (i.e., where wizards write to and miners
	 * read from).
	 * 
	 * @param message
	 *            message to be put on the channel
	 */
	public void putMessageWizardChannel(Message message) {
		
		int currentRoom = message.getCurrentRoom();
		String data = message.getData();
		long tid = Thread.currentThread().getId();
		
		//mesajul ce va fi trimis pe canal
		Message newMessage = null;
		
		//mesajele END pot fi ignorate
		if(data.equals(END)) return;
		
		//se vor pune mesaje EXIT de un singur thread pana se inchid toti Miners
		
		if(data.equals(EXIT)) {
			if(finishThread == -1) {
				finishThread = tid;
			}else {
				if(finishThread != tid) return;
			}
			
			newMessage = message;
		}	
		//daca sunt aduse informatiile despre parinte
		else if((!map.containsKey(tid))) {	
			if(data.equals(NO_PARENT)) currentRoom = -1;
			newMessage = new Message(currentRoom,-1,null);
			map.put(tid, newMessage);
			return;
		//daca au fost aduse informatiile despre parinte si sunt adusa informatii despre un nod adiacent
		}else if(map.containsKey(tid)) {
			newMessage = map.remove(tid);
			newMessage.setCurrentRoom(currentRoom);
			newMessage.setData(data);
			
			//nu se trimite acelasi nod de mai multe ori
			if(putNodes.contains(currentRoom)) return;
			else putNodes.add(currentRoom);
		}
		
		try {
			//se pune mesajul construit in buffer
			bufferWizardChannel.put(newMessage);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}

	/**
	 * Gets a message from the wizard channel (i.e., where wizards write to and
	 * miners read from).
	 * 
	 * @return message from the miner channel
	 */
	public Message getMessageWizardChannel() {
		Message temp = null;
		
		try {
			//se ia mesajul din buffer
			temp = bufferWizardChannel.take();	
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return temp;
		
	}
}
