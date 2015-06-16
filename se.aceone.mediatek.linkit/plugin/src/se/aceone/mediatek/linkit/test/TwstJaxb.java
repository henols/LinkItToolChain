package se.aceone.mediatek.linkit.test;
	

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import se.aceone.mediatek.linkit.xml.proj.VisualStudioProject;
import se.aceone.mediatek.linkit.xml.proj.VisualStudioProject.Files;
import se.aceone.mediatek.linkit.xml.proj.VisualStudioProject.Files.Filter;
import se.aceone.mediatek.linkit.xml.proj.VisualStudioProject.Files.Filter.File;

public class TwstJaxb {

	public static void main(String[] args) {

		 try {
	 
			java.io.File file = new java.io.File("../proj.xml");
			JAXBContext jaxbContext = JAXBContext.newInstance(VisualStudioProject.class);
	 
			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
			VisualStudioProject packageinfo = (VisualStudioProject) jaxbUnmarshaller.unmarshal(file);
			System.out.println(packageinfo.getName());
			Files files = packageinfo.getFiles();
			for(Filter f: files.getFilter()){
				System.out.println(f.getName() +" "+
			f.getFilter());
				for(File fx:f.getFile()){
					System.out.println(fx.getRelativePath());
				}
			}
		  } catch (JAXBException e) {
			e.printStackTrace();
		  }
	 
	}

}
