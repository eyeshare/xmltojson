package xm.json.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.org.apache.bcel.internal.generic.NEW;

import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JsonConfig;
import net.sf.json.util.JSONBuilder;

@SuppressWarnings("all")
public class PropertyControlParser {
	
	public static Document mergeXML(String src, String target) throws IOException{
		InputStream inputStream= PropertyControlParser.class.getResourceAsStream(src);
		Document srcDocument=XMLHelper.getDocumentFromStream(inputStream);
		inputStream.close();
		
		inputStream=PropertyControlParser.class.getResourceAsStream(target);
		Document targetDocument=XMLHelper.getDocumentFromStream(inputStream);
		inputStream.close();
		
		Node srcNode=getDocumentFirstElement(srcDocument);
		Node targetNode=getDocumentFirstElement(targetDocument);
		NodeList nodes=targetNode.getChildNodes();
		for(int i=0,l=nodes.getLength();i<l;i++){
			srcNode.appendChild(srcDocument.importNode(nodes.item(i), true));
		}
		return srcDocument;
	}
	
	public static Node getDocumentFirstElement(Document document){
		Node root=document.getDocumentElement();
		return root;
	}
	
	public static Map<String,List<Node>> getChildElement(Node node){
		Map<String,List<Node>> children=new HashMap<String,List<Node>>();
		NodeList nodes= node.getChildNodes();
		Node next=null;
		for(int i=0,j=nodes.getLength();i<j;i++){
			next=nodes.item(i);
			if(next.getNodeType()==Node.ELEMENT_NODE){
				if(children.containsKey(next.getNodeName())){
					children.get(next.getNodeName()).add(next);
				}else{
					List list=new ArrayList();
					list.add(next);
					children.put(next.getNodeName(), list);
				}
			}
		}
		return children;
	}
	
	public static Map<String,String> getNodeText(Node node){
		NodeList nodes=node.getChildNodes();
		Map<String,String> result=new HashMap<String,String>();
		if(nodes==null||nodes.getLength()==0){
			result.put(node.getNodeName(), "null");
		}else{
			Node text;
			for(int i=0,j=nodes.getLength();i<j;i++){
				text=nodes.item(i);
				if(text.getNodeType()==Node.TEXT_NODE){
					String value=text.getNodeValue();
					if(value!=null&&value.indexOf("\n")>-1){
						continue;
					}
					result.put(node.getNodeName(),text.getNodeValue());
				}
			}
		}
		
		return result;
	}
	
	public static Node getElementById(Document document,String id,String tag){
		NodeList list= document.getElementsByTagName(tag);
		Node node=null;
		Map<String,String> attributes;
		for(int i=0,j=list.getLength();i<j;i++){
			node=list.item(i);
			attributes=getNodeAttribute(node);
			if(attributes.containsKey("id")&&attributes.get("id").equalsIgnoreCase(id)&&node.getNodeName().equalsIgnoreCase(tag)){
				return node;
			}
		}
		return node;
	}
	
	public static Map<String,String> getNodeAttribute(Node node){
		NamedNodeMap attrs=node.getAttributes();
		Node attr;
		String key,value;
		Map<String,String> result=new HashMap<String,String>();
		for(int i=0,j=attrs.getLength();i<j;i++){
			attr=attrs.item(i);
			key=attr.getNodeName();
			value=attr.getNodeValue();
			result.put(key, value);
		}
		return result;
	}
	
	public static Map getNodeAttrAndText(Node node){
		Map map=getNodeText(node);
		Map attr=getNodeAttribute(node);
		map.forEach((tag,text)->{
			attr.put(tag,text);
		});
		return attr;
	}
	
	public static boolean hasChildElementAndArray(Node node){
		boolean isLeafNode=isLeafNode(node);
		if(isLeafNode){
			return false;
		}else{
			Map<String,List<Node>> children=getChildElement(node);
			boolean isArray=hasChildElementAndArray(children);
			if(isArray){
				return true;
			}
		}
		return false;
	}
	
	public static boolean hasChildElementAndArray(Map<String,List<Node>> children){
		List<Integer> tagCount=new ArrayList();
		List<Integer> childCount=new ArrayList();
		children.forEach((tag,list)->{
			if(tagCount.isEmpty()){
				tagCount.add(Integer.valueOf(1));
			}else{
				tagCount.set(0, Integer.valueOf(tagCount.get(0).intValue()+1));
			}
			list.forEach((child)->{
				if(childCount.isEmpty()){
					childCount.add(Integer.valueOf(1));
				}else{
					childCount.set(0, Integer.valueOf(childCount.get(0).intValue()+1));
				}
			});
		});
		if(!children.isEmpty()&&tagCount.get(0).intValue()==1&&childCount.get(0).intValue()!=1){
			return true;
		}
		return false;
	}
	
	public static boolean isLeafNode(Node node){
		Map<String,List<Node>> children=getChildElement(node);
		Map<String,Boolean> isLeaf=new HashMap<String,Boolean>();
		isLeaf.put("leaf", false);
		children.forEach((tag,list)->{
			list.forEach((child)->{
				Map nodeText=getNodeText(child);
				if(!nodeText.isEmpty()){
					isLeaf.put("leaf", true);
				}
			});
		});
		
		return isLeaf.get("leaf");
	}
	
	public static String xmlToJSON(Document document){
		JSONObject json=new JSONObject();
		Node node=document.getDocumentElement();
		createJSONObject(node, json,new JSONArray());		
		return json.toString();
	}
	
	public static void createJSONObject(Node root,JSONObject jsonObject,JSONArray jsonArray){
		Map<String,List<Node>> children=getChildElement(root);
		if(!children.isEmpty()){
			boolean isLeaf=isLeafNode(root);
			if(isLeaf){
				boolean hasChildArray=hasChildElementAndArray(children);
				if(hasChildArray) {
					children.forEach((tag,list)->{
						list.forEach((child)->{
							JsonConfig config=new JsonConfig();
					        config.setIgnoreDefaultExcludes(true);
							JSONObject obj=new JSONObject();
							obj.accumulate(child.getNodeName(), "",config);
							jsonArray.add(obj);
						});
					});
					jsonObject.accumulate(root.getNodeName(), jsonArray);
					jsonArray.clear();
					
					JSONObject refJSON=new JSONObject();
					createJSONObjectByRefTree(root, refJSON);
					JSONArray array =jsonObject.getJSONArray(root.getNodeName());
					array.clear();
					String key=refJSON.keys().next().toString();
					array.addAll(refJSON.getJSONArray(key));
				}else {
					JSONObject subJSON=new JSONObject();
					createJSONObjectByLeaf(root, subJSON);
					copyJSON(subJSON, jsonObject);
					JSONObject refJSON=new JSONObject();
					createJSONObjectByRefTree(root, refJSON);
					mergeJSONObject(refJSON, jsonObject);
					jsonObject.clear();
					copyJSON(refJSON, jsonObject);
				}
			}else{
				boolean hasChildArray=hasChildElementAndArray(root);
				if(hasChildArray){
					children.forEach((tag,list)->{
						list.forEach((child)->{
							JSONObject object=new JSONObject();
							JSONArray array=new JSONArray();
							createJSONObject(child, object,array);
							
							JSONObject refJSON=new JSONObject();
							createJSONObjectByRef(child, refJSON);
							if(!refJSON.isEmpty()) {
								if(!object.isEmpty()) {
									JSONObject json=object.getJSONObject(object.keys().next().toString());
									copyJSON(json, refJSON);
									object.clear();
									copyJSON(refJSON, object);
									jsonArray.element(object);
								}
							}else {
								if(!object.isEmpty()) {
									jsonArray.element(object);
								}
							}
						});
						JsonConfig config=new JsonConfig();
				        config.setIgnoreDefaultExcludes(true);
				        config.setIgnorePublicFields(true);
						jsonObject.accumulate(root.getNodeName(), jsonArray,config);
						jsonArray.clear();
					});
				}else{
					children.forEach((tag,list)->{
						list.forEach((child)->{
							JSONObject object=new JSONObject();
							JSONArray array=new JSONArray();
							createJSONObject(child,object,array);
							if(!object.isEmpty()) {
								if(jsonObject.containsKey(root.getNodeName())) {
									copyJSON(object, jsonObject.getJSONObject(root.getNodeName()));
								}else {
									jsonObject.put(root.getNodeName(), object);
								}
								JSONObject refJSON=new JSONObject();
								createJSONObjectByRefTree(root, refJSON);
							}
						});
					});
				}
			}
			
		}
	}
	
	public static void mergeJSONObject(JSONObject refJSON,JSONObject sourceJSON) {
		sourceJSON.forEach((k,v)->{
			Object object=refJSON.get(k);
			if(object instanceof JSONObject) {
				if(((JSONObject)object).isEmpty()) {
					refJSON.put(k, v);
				}
			}
		});
	}
	

	public static void createJSONObjectByLeaf(Node node,JSONObject jsonObject) {
		boolean isLeaf=isLeafNode(node);
		if(isLeaf) {
			Map<String, List<Node>> children=getChildElement(node);
			children.forEach((tag,list)->{
				list.forEach((child)->{
					Map<String, String> text=getNodeText(child);
					if(!text.isEmpty()) {
						copyTextToJSON(text, jsonObject);
					}else {
						boolean isSubLeaf=isLeafNode(child);
						if(isSubLeaf) {
							JSONObject subJSON=new JSONObject();
							createJSONObjectByLeaf(child, subJSON);
							if(!subJSON.isEmpty()) {
								jsonObject.accumulate(child.getNodeName(), subJSON);
							}
						}else {
							JSONObject subJSON=new JSONObject();
							JSONArray subArray=new JSONArray();
							createJSONObject(child, subJSON, subArray);
							if(!subJSON.isEmpty()) {
								jsonObject.accumulate(child.getNodeName(), subJSON);
							}
						}
					}
				});
			});
		}
	}
	
	
	public static void createJSONObjectByRef(Node node,JSONObject jsonObject) {
		Map<String, String> attribute=getNodeAttribute(node);
		if(!attribute.isEmpty()&&attribute.containsKey("ref")) {
			String nodeId=attribute.get("ref");
			Node refNode=getElementById(node.getOwnerDocument(), nodeId, node.getNodeName());
			JSONObject subObject=new JSONObject();
			JSONArray subArray=new JSONArray();
			createJSONObject(refNode, subObject, subArray);
			if(!subObject.isEmpty()) {
				copyJSON(subObject, jsonObject);
			}				
		}
	}
	
	
	public static void createJSONObjectByRefTree(Node node,JSONObject jsonObject) {
		createJSONObjectByRef(node,jsonObject);
		Map<String,List<Node>> children=getChildElement(node);
		if(!children.isEmpty()) {
			children.forEach((tag,list)->{
				list.forEach((child)->{
					Map<String, List<Node>> subChildren=getChildElement(child);
					if(!subChildren.isEmpty()) {
						JSONObject subJSON=new JSONObject();
						createJSONObjectByRefTree(child, subJSON);
						if(subJSON.keySet().size()==1&&subJSON.containsKey(child.getNodeName())) {
							jsonObject.accumulate(child.getNodeName(),subJSON.get(child.getNodeName()));
						}else {
							jsonObject.accumulate(child.getNodeName(), subJSON);
						}
					}else{
						JSONObject subJSON=new JSONObject();
						createJSONObjectByRef(child, subJSON);
						if(subJSON.keySet().size()==1&&subJSON.containsKey(child.getNodeName())) {
							Object object=subJSON.get(child.getNodeName());
							if(object instanceof JSONArray||object instanceof JSONObject) {
								jsonObject.accumulate(child.getNodeName(),subJSON.get(child.getNodeName()));
							}
						}else {
							jsonObject.accumulate(child.getNodeName(), subJSON);
						}
					}
				});
			});
		}
	}
	
	
	private static void copyJSON(JSONObject src,JSONObject target){
		src.keySet().forEach((key)->{
			target.put(key, src.get(key));
		});
	}
	
	
	private static void copyTextToJSON(Map<String,String> text,JSONObject json){
		JsonConfig config=new JsonConfig();
        config.setIgnoreDefaultExcludes(true);
		text.forEach((k,v)->{
			if(v!=null){
				json.accumulate(k, v,config);
			}
		});
	}
	
	
	private static void copyTextToJSON(Map<String,String> text,JSONObject json,boolean isreset) {
		text.forEach((k,v)->{
			if(v!=null){
				if(json.containsKey(k)) {
					Object obj=json.get(k);
					if(obj instanceof JSONArray) {
						json.getJSONArray(k).add(v);
					}
				}else {
					json.put(k, v);
				}
			}
		});
	}
}
