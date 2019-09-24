package xm.json.utils;

import java.io.IOException;

import org.w3c.dom.Document;

public class Demo {

	public static void main(String[] args) throws IOException {
		String property="/xm/json/property/properties.xml";
		String button="/xm/json/property/controls_Button.xml";
		Document document=PropertyControlParser.mergeXML(property, button);
		String doc=XMLHelper.getStringFromDocument(document);
		System.out.println(doc);
		
		String json=PropertyControlParser.xmlToJSON(document);
		System.out.println(json);
	}

}
