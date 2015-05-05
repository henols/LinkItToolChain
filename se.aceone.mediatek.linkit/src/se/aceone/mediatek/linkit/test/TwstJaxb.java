package se.aceone.mediatek.linkit.test;
	
import java.io.File;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import se.aceone.mediatek.linkit.xml.config.Packageinfo;

public class TwstJaxb {

	public static void main(String[] args) {

		 try {
	 
			File file = new File("config.xml");
			JAXBContext jaxbContext = JAXBContext.newInstance(Packageinfo.class);
	 
			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
			Packageinfo packageinfo = (Packageinfo) jaxbUnmarshaller.unmarshal(file);
			System.out.println(packageinfo.getUserinfo().getAppname());
	 
		  } catch (JAXBException e) {
			e.printStackTrace();
		  }
	 
	}

}
