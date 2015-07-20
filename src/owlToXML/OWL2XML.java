/**
 * 
 */
package owlToXML;

import java.io.File;

import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.PrefixManager;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;
import org.w3c.dom.Document;
import org.w3c.dom.Element; 
import org.w3c.dom.Text;

/**
 * @author vasant
 * this tool should read DIAB onto OWL
 * file and write it out as .mwt file. 
 *
 */
public class OWL2XML {
	
	private OWLOntology localDiab;
	private OWLOntologyManager manager;
	private OWLDataFactory factory;
	private Document doc; 
	private Element root;
	
	
	public void readFile(){
		this.manager = OWLManager.createOWLOntologyManager();
		this.factory = manager.getOWLDataFactory();
		File file = new File("/Users/vasant/OntologyFiles/Diabetes_Ontology_V33.owl");
		try {
			this.localDiab = manager.loadOntologyFromOntologyDocument(file);
		} catch (OWLOntologyCreationException e) {
			System.out.println("Something went wrong in loading the ontology");
			e.printStackTrace();
			}
	}

	
	public void readAnnotations(){
		
		this.factory = manager.getOWLDataFactory();
		PrefixManager pm = new DefaultPrefixManager("http://www.geneontology.org/formats/oboInOwl");
		OWLAnnotationProperty property = factory.getOWLAnnotationProperty("#hasExactSynonym", pm);
		OWLAnnotationProperty label = factory.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI());
		OWLReasonerFactory rf = new Reasoner.ReasonerFactory();
		OWLReasoner reasoner = rf.createReasoner(localDiab);
		for(OWLClass cls : localDiab.getClassesInSignature()){
			if(cls.getIRI().toString().contains("http://purl.obolibrary.org/obo/DIAB_000006")){
				//SET THE SUBCLASS FLAG TO FALSE SINCE I WANT JUST THE DIRECT SUBCLASSES 
				NodeSet<OWLClass> diabSubclasses = reasoner.getSubClasses(cls, true);
				for(OWLClass diabsubcls : diabSubclasses.getFlattened()){
					String id = diabsubcls.getIRI().toString();
					System.out.println("url of the subclass is " + id);
					String shortid = id.replace("http://purl.obolibrary.org/obo/", "");
					System.out.println("Id of the subclass is " + shortid);
					for (OWLAnnotation annotation : diabsubcls.getAnnotations(localDiab, label)){
						if(annotation.getValue() instanceof OWLLiteral){
							OWLLiteral lab = (OWLLiteral) annotation.getValue();
							String labe = lab.getLiteral();
							System.out.println("Label is "+ labe);
							createXML(shortid,labe);
						}
					}
					for(OWLAnnotation annotation1 : diabsubcls.getAnnotations(localDiab, property)){
						if(annotation1.getValue() instanceof OWLLiteral){
							OWLLiteral syn = (OWLLiteral) annotation1.getValue();
							String synonym = syn.getLiteral();
							System.out.println("Exact Synonym is " + synonym);
							createXML(shortid, synonym);
						}
					}
				}
				
				
			}
			
		}
		
		
	}
	
	
	
	
	public void createHeaderXML(){

        try {

            //Creating an empty XML Document

            //We need a Document
            DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
            this. doc = docBuilder.newDocument();
            //create the root element and add it to the document
            root = doc.createElement("mwt");
            doc.appendChild(root);
            //create the template element <template><z:hgnc ids='%1'>%0</z:hgnc></template>
            Element template = doc.createElement("template");
            root.appendChild(template);
            Element zhgnc = doc.createElement("z:DIAB");
            zhgnc.setAttribute("ids", "%1");
            template.appendChild(zhgnc);
            Text text = doc.createTextNode("%0");
            zhgnc.appendChild(text);

        }catch (Exception e) {
            System.out.println(e);
        }
}


public void createXML(String hgncid, String value){

    //create child element, add an attribute, and add to root
    Element child = doc.createElement("t");
    child.setAttribute("p1", hgncid);
    root.appendChild(child);
    //add a text element to the child
    Text text = doc.createTextNode(value);
    child.appendChild(text);
}

public void createFootXML(){
    /**<template>%0</template>
<r>&lt;z:[^&gt;]*&gt;(.*&lt;/z)!:[^&gt;]*></r>
<r>&lt;protname[^&gt;]*&gt;(.*&lt;/protname)![^&gt;]*></r>
    <!-- next one to skip anything looking like an html tag -->
    <r>&lt;/?[A-Za-z_0-9\-]+(&gt;|[\r\n\t ][^&gt;]+)</r>*/

      Element template = doc.createElement("template");
      root.appendChild(template);
      Text text = doc.createTextNode("%0");
      template.appendChild(text);

      Element r1 = doc.createElement("r");
      root.appendChild(r1);
      Text text1 = doc.createTextNode("&lt;z:[^&gt;]*&gt;(.*&lt;/z)!:[^&gt;]*>");
      r1.appendChild(text1);

      Element r2 = doc.createElement("r");
      root.appendChild(r2);
      Text text2 = doc.createTextNode("&lt;protname[^&gt;]*&gt;(.*&lt;/protname)![^&gt;]*>");
      r2.appendChild(text2);

      Element r3 = doc.createElement("r");
      root.appendChild(r3);
      Text text3 = doc.createTextNode("&lt;/?[A-Za-z_0-9\\-]+(&gt;|[\r\n\t ][^&gt;]+)");
      r3.appendChild(text3);

}

public void saveXML (){

    // write the content into xml file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer;
            try {
                transformer = transformerFactory.newTransformer();
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File("/Users/vasant/OntologyFiles/diab.mwt"));

            // Output to console for testing
            // StreamResult result = new StreamResult(System.out);

            transformer.transform(source, result);

            System.out.println("File saved!");
            }
            catch (TransformerConfigurationException e) {
                System.out.println(e);
                e.printStackTrace();
            } catch (TransformerException e) {
                System.out.println(e);
                e.printStackTrace();
            }

}

	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		OWL2XML obj = new OWL2XML();
		obj.createHeaderXML();
		obj.readFile();
		obj.readAnnotations();
		obj.createFootXML();
		obj.saveXML();
	}

}
