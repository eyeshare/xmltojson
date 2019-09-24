package xm.json.utils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

public class XMLHelper {
	
	public static Document getDocumentFromStream(InputStream is){
		Document doc = null;
		try {
			DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
			String FEATURE = "http://apache.org/xml/features/disallow-doctype-decl";
			builderFactory.setFeature(FEATURE, true);
			DocumentBuilder builder = builderFactory.newDocumentBuilder();
			doc = builder.parse(is);
			return doc;
		} catch (Exception e) {
			System.out.println("getDocumentFromStream执行异常"+ e);
		}
		return null;
	}
	
	public static String getStringFromDocument(Document doc){
		try{
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer t = tf.newTransformer();
			t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			t.setOutputProperty(OutputKeys.INDENT, "yes");
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			t.transform(new DOMSource(doc), new StreamResult(bos));
			String xmlcontent = bos.toString("UTF-8");
			return xmlcontent;
		}catch(Exception e){
			System.out.println("getStringFromDocument执行异常"+e);
		}
		return null;
	}
}
