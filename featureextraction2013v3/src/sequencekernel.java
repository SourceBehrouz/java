import java.io.File;
import java.io.FileNotFoundException;
import java.io.Reader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import javax.xml.parsers.ParserConfigurationException;
import org.annolab.tt4j.*;

import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_problem;

import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.objectbank.TokenizerFactory;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.Tokenizer;
import edu.stanford.nlp.process.WordTokenFactory;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.CoreMap;
import edu.ucdenver.ccp.nlp.biolemmatizer.BioLemmatizer;


public class sequencekernel {

	/**
	 * @param args
	 * @throws IOException 
	 */
	
    static int numberofPairs = 28000;
    static String[][][] PairContextArray = new String[numberofPairs][13][];
    static String[] PairsArray = new String[numberofPairs];
	static int numberoftestPairs= 2500;
	static int Fold=10;
	static int maxnegativekernel = 117;
	static int sizeofbagofwords= 2589;
//   int[][] PairsBagofWords = new int[numberofPairs][maxSentenceTokens];
//    byte[][] reducedPairsBagofWords = new byte[numberofPairs][1200];
    static byte[] pairhasrelation = new  byte[numberofPairs] ;
    static byte[][] PairsAllbagofwords = new byte[numberofPairs][sizeofbagofwords];
	private static boolean[] pairhasnegation = new boolean[numberofPairs];
	static LexicalizedParser lp =
	new LexicalizedParser("Data/englishPCFG.ser.gz");

	
	
	public static String subSetTree(Tree t,int firstindex1,int secondindex1) {
    		int index = 0;
			String  tAllSubsetstring = "";
			List<Tree> TList = t.subTreeList();
			while (index != TList.size()){
			Tree tindex = TList.get(index);	
			String tindexstring = "";
			for (int i = 0; i< tindex.preOrderNodeList().size();i ++){
		    tindexstring = tindexstring + tindex.preOrderNodeList().get(i).value().replaceAll("\\s","");
			}
			tAllSubsetstring = tAllSubsetstring +" "+ tindexstring;
			index++;
			}
		return tAllSubsetstring;
	}
	
	public static int pspectrumkernel(String s1, String s2, int p) throws IOException{

		
		
		/*	
	    -The following algorithm is used:
	%         K[p](sa,t) = K[p](s,t) + [Summation of i from 1 to |t|] G[p-1](s,t(1:i-1)) [t(i) == a]
	%           K[p](s,t) = 0 if |s| < p  or |t| < p
	%         G[p](sa, tb) = G[p-1](s,t)[a==b]
	%           G[0](s,t) = 1 for all s,t
	%           G[p](s,t) = 0 if |s| == 0  or |t| == 0
	%         
	*/
		int sum = 0;
		if ((s1.length() < p) ||(s2.length() < p)) sum =0; else
			if (p==0) sum=1; else if ((s1.length() ==0) ||(s2.length() ==0)) sum=0; else if (s1.charAt(s1.length()-1) == s2.charAt(s2.length()-1)) 
				sum = pspectrumkernel(s1.substring(0, s1.length()-2),s2.substring(0, s2.length()-2), p-1);
			else {
				char a = s1.charAt(s1.length()-1);
				int summation =0;
				for (int i=0;i< s2.length();i++)
					if (s2.charAt(i)==a) summation = summation + pspectrumkernel(s1,s2.substring(0, i), p-1);
					sum = pspectrumkernel(s1.substring(0, s1.length()-2),s2, p-1) + summation;
			}
		
		return sum;
	}
	

public static void main(String[] args) throws IOException, ClassNotFoundException, TreeTaggerException  {
		// TODO Auto-generated method stub
		// TODO Auto-generated method stub
        FileWriter outFile = null;
        double f_1=0;
        int pairnumbers=0;
        int pairnumber =-1;
        int negativepairs=0;
        int positivepairs=0;
        int leftDrugTokenPosition=0;
        int rightDrugTokenPosition=0;
        Arrays.fill(pairhasnegation, Boolean.FALSE);
        MaxentTagger tagger = new MaxentTagger("models/left3words-wsj-0-18.tagger");
		try {
			outFile = new FileWriter("druginformation.txt");
		} catch (IOException e3) {
			// TODO Auto-generated catch block
			e3.printStackTrace();
		}
		final PrintWriter outwekafile = new PrintWriter(outFile);
	

      class wordrecord {
        	String word = null;
        	int numbers = 0;
        }
		try {
			outFile = new FileWriter("allPairs.txt");
		} catch (IOException e3) {
			// TODO Auto-generated catch block
			e3.printStackTrace();
		}
     PrintWriter outPairs = new PrintWriter(outFile);
     String path = "./Corpus"; 
		String files = null;
		org.w3c.dom.Document doc = null;
		File folder = new File(path);
	
		
		try {
			outFile = new FileWriter("druginformationpos.txt");
		} catch (IOException e3) {
			// TODO Auto-generated catch block
			e3.printStackTrace();
		}
		final PrintWriter outposfile = new PrintWriter(outFile);
	
		try {
			outFile = new FileWriter("druginformationneg.txt");
		} catch (IOException e3) {
			// TODO Auto-generated catch block
			e3.printStackTrace();
		}
		PrintWriter outnegfile = new PrintWriter(outFile);
		
		
		File[] listOfFiles = folder.listFiles(); 
		Arrays.sort(listOfFiles);
   NodeList[] AllPairs = new NodeList[listOfFiles.length];
		int numSections = 0;
		javax.xml.parsers.DocumentBuilderFactory docFactory =   javax.xml.parsers.DocumentBuilderFactory.newInstance();
     javax.xml.parsers.DocumentBuilder docBuilder = null;
     wordrecord[] bagofwords = new wordrecord[sizeofbagofwords]; 
//     wordrecord[] reducedbagofwords = new wordrecord[1200];
     for (int i = 0; i < sizeofbagofwords; i++) bagofwords[i] = new wordrecord();
		try {
			docBuilder = docFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		for (int i =0;i< numberofPairs;i++) pairhasrelation[i]= 0;
 		// Traverse all files in corpus folder   	    		
		for (int fileindex = 0; fileindex < listOfFiles.length; fileindex++) 
		  { 		 
		   if (listOfFiles[fileindex].isFile()) 
		   {
		   files = listOfFiles[fileindex].getName();
		   System.out.println(files+"**"+ fileindex );
		      }
     docFactory =   javax.xml.parsers.DocumentBuilderFactory.newInstance();
     docBuilder = null;
     try {
       docBuilder = docFactory.newDocumentBuilder();
     } catch (ParserConfigurationException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
     }
         try {
   			String xmlfilePath = "./Corpus/"+ files;
             try {
					doc = docBuilder.parse(xmlfilePath);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
             doc.getDocumentElement().normalize();
         } catch (SAXException e) {
             // TODO Auto-generated catch block
             e.printStackTrace();
         } 
 		NodeList nl;
 		NodeList n2;
   for (int i1 = 0; i1< doc.getElementsByTagName("sentence").getLength(); i1++){
        nl = doc.getElementsByTagName("sentence");
		if(nl != null && nl.getLength() > 0) {
			Element elSentence = (Element)nl.item(i1);
			System.out.println("numberofentytiesandPairs="+elSentence.getChildNodes().getLength());
			//point: below condition will remove useless Pairs
//			if (elSentence.getChildNodes().getLength()==0) System.out.println("ZZZZZZZZZZZZZZZZZZZZZZZZZ");else
     	AllPairs[fileindex] =  doc.getElementsByTagName("sentence");
		String sentenceid = elSentence.getAttributes().getNamedItem("id").getNodeValue();
    	
 //		for(int index=0;index < elSentence.getChildNodes().getLength();index++) 
//		{
		n2 = elSentence.getElementsByTagName("pair");
		if (n2.getLength() > 0){
	    	pairnumbers = pairnumbers + n2.getLength();
			for (int j=0;j<n2.getLength();j++){
		     	pairnumber++;
			String b = n2.item(j).getAttributes().getNamedItem("ddi").getNodeValue();
			if (b.equals("true")) 
			{String type = n2.item(j).getAttributes().getNamedItem("type").getNodeValue();
			if (type.equalsIgnoreCase("advice"))  pairhasrelation[pairnumber] =1;
			else if (type.equalsIgnoreCase("effect"))  pairhasrelation[pairnumber] =2; 
			else if (type.equalsIgnoreCase("mechanism"))  pairhasrelation[pairnumber] =3; 
			else if (type.equalsIgnoreCase("int"))  pairhasrelation[pairnumber] =4; 
			positivepairs++;
			}  else negativepairs++;
			String e1 = n2.item(j).getAttributes().getNamedItem("e1").getNodeValue();
			String e2 = n2.item(j).getAttributes().getNamedItem("e2").getNodeValue();
			String firstdrugname = null;
			String Seconddrugname = null;
			int firstdrugindex = 0;
			int seconddrugindex = 0;
			for (int in=0;in < elSentence.getElementsByTagName("entity").getLength();in++){
			String s1 = elSentence.getElementsByTagName("entity").item(in).getAttributes().getNamedItem("id").getNodeValue();	//	elSentence.
			if (s1.equals(e1)) {
				 firstdrugname = elSentence.getElementsByTagName("entity").item(in).getAttributes().getNamedItem("text").getNodeValue();	//	elSentence.
				 firstdrugindex = in;
			}
			if (s1.equals(e2)) {
				 Seconddrugname = elSentence.getElementsByTagName("entity").item(in).getAttributes().getNamedItem("text").getNodeValue();	//	elSentence.
					seconddrugindex = in;
			}
			}
	//		if (!(firstdrugname.contains("Drug"))&&!(Seconddrugname.contains("Drug"))&&!(firstdrugname.equalsIgnoreCase("drugs"))&&!(Seconddrugname.equalsIgnoreCase("drug"))&&(!firstdrugname.equalsIgnoreCase(Seconddrugname)))
			{ 

       ///////////////////////////////////////context kernel////////////////////////////////////////////
		String  Pairstringtemp = elSentence.getAttributes().getNamedItem("text").getNodeValue();
	String Pairstring = Pairstringtemp.replace(firstdrugname, "DrugName");
		Pairstring = Pairstring.replace(Seconddrugname, "DrugName");
		System.out.println("pair="+n2.item(j).getAttributes().getNamedItem("id").getNodeValue());
    	System.out.println((pairnumber+1)+" "+negativepairs+" "+ positivepairs+" "+Pairstring+" "+firstdrugname+" "+Seconddrugname);
     	int index = Pairstring.indexOf("DrugName");
     	int indexlast = Pairstring.lastIndexOf("DrugName");
    	String LeftContext = null;
     	String MiddleContext = null;
     	String RightContext = null;
    	if (index < 0){
         	String[]  charOffset = elSentence.getElementsByTagName("entity").item(firstdrugindex).getAttributes().getNamedItem("charOffset").getNodeValue().split("[-;]");
         	String s1 = charOffset[0];
         	String s2 = charOffset[1];
         	Pairstring = Pairstring.replace(Pairstring.subSequence(Integer.parseInt(s1), Integer.parseInt(s2)),"DrugName");
            index = Pairstring.indexOf("DrugName");
   	}
         if   (indexlast >= Pairstring.length()|(indexlast < 0))	{
         	String[]  charOffset2 = elSentence.getElementsByTagName("entity").item(seconddrugindex).getAttributes().getNamedItem("charOffset").getNodeValue().split("[-;]");
         	String s3 = charOffset2[0];
         	String s4 = charOffset2[1];
         	Pairstring = Pairstring.replace(Pairstring.subSequence(Integer.parseInt(s3), Integer.parseInt(s4)),"DrugName");
            indexlast = Pairstring.lastIndexOf("DrugName");
        	}
         PairsArray[pairnumber] = Pairstring;
         if (index > indexlast) {
        	 int temp = index;
        	 index = indexlast;
        	 indexlast = temp;
         }
         int st = 0;
         if (index+8 > indexlast) st = 0; else st = 8;
         Pairstring = Pairstring.replaceAll(",", " # ");
         Pairstring = Pairstring.replaceAll(";", " | ");
         Pairstring = Pairstring.replaceAll("[?]", " @ ");
 		 Pairstring = Pairstring.replaceAll("[\r\n]+", "");
 		 Pairstring = Pairstring.replaceAll("'"," ");
 		 Pairstring = Pairstring.replaceAll("&quot"," ");
 		 Pairstring = Pairstring.replaceAll("\""," ");
 		 Pairstring = Pairstring.replaceAll("\'"," ");
	
    	 LeftContext = Pairstring.substring(0, index);
     	 MiddleContext = Pairstring.substring(index+st , indexlast);
     	 RightContext = Pairstring.substring(indexlast+8, Pairstring.length());
 ///////////////////////// finding negation ///////////////////
     	String LeftContextneg = null;
      	String MiddleContextneg = null;
     	String RightContextneg = null;
     	 
 
     	 
  ////////////////////////////////
     	 
     	 
     	 String delims = "[ ,;.!*^/?(){}<>:]+";
       final Stem stem = new Stem("stemrules.txt", "/p");
 //      String[] token = s1.split("((?<="+delims+")|(?="+delims+"))");
  //     String[] token2 = s2.split("((?<="+delims+")|(?="+delims+"))");
//       LeftContext = stem.stripAffixes(LeftContext);
       //  if (Pairstring.startsWith(delims))
         String Pairstring2 = Pairstringtemp;
 		 Pairstring2 = Pairstring2.replaceAll("[\r\n]+", "");
 		 Pairstring2 = Pairstring2.replaceAll("'"," ");
 		 Pairstring2 = Pairstring2.replaceAll("&quot"," ");
 		 Pairstring2 = Pairstring2.replaceAll("\""," ");
 		 Pairstring2 = Pairstring2.replaceAll("\'"," ");
	 		
 		Pairstring2 = Pairstring2.replace(firstdrugname, "DrugNameF");
 	  Pairstring2 = Pairstring2.replace(Seconddrugname, "DrugNameS");
     	 Pairstring2 = Pairstring2.replaceAll("^"+delims,"");
     	System.out.println(Pairstring2);

//////////////////stanford tokenizer ////////////////////////
Reader r = new StringReader(Pairstring2); // initialized somehow by you
Tokenizer<CoreLabel> tokenizer = new PTBTokenizer<CoreLabel>(r, new CoreLabelTokenFactory(), "normalizeFractions=true");
List<String> pairstringtokensList = new  ArrayList<String>();
List<Word> pairstringWord = new  ArrayList<Word>();
int i2 = 0;
while (tokenizer.hasNext()) {
CoreLabel token = tokenizer.next();
   pairstringtokensList.add(token.word()) ;
   Word w = new Word();w.setValue(token.word());
   pairstringWord.add(w);
i2++;
}
String[] pairstringtokens = new String[i2];
for (int l=0;l<i2;l++){
pairstringtokens[l] = pairstringtokensList.get(l);
//System.out.println(pairstringtokens[l]);
}
//////////////////stanford tokenizer ////////////////////////
leftDrugTokenPosition = 0;
rightDrugTokenPosition = pairstringtokens.length-1;

         for (int i=0;i<pairstringtokens.length;i++){
        if 	 (pairstringtokens[i].equalsIgnoreCase("DrugNameF")) {
        	pairstringtokens[i] = firstdrugname; 
        	leftDrugTokenPosition = i;
        }else
        
        	if (pairstringtokens[i].equalsIgnoreCase("DrugNameS")) {
        		pairstringtokens[i] = Seconddrugname;
            rightDrugTokenPosition = i;
       	
        	}
         }
	     String[] Lefttokens = LeftContext.split(delims);
		PairContextArray[pairnumber][0] = Lefttokens;
	//	MiddleContextstem = stem.stripAffixes(MiddleContext);
	     String[] Middletokens = MiddleContext.split(delims);
		PairContextArray[pairnumber][1] = Middletokens;
//		RightContext = stem.stripAffixes(RightContext);
	     String[] Righttokens = RightContext.split(delims);
		PairContextArray[pairnumber][2] = Righttokens;
		int sub=3;
        if (Lefttokens.length != 0 ) {
        	if ((Lefttokens.length == 1 ))
        	sub = 1; else if (Lefttokens.length == 2 ) sub = 2;
        	PairContextArray[pairnumber][3] = Arrays.copyOfRange(Lefttokens, Lefttokens.length-sub, Lefttokens.length);
        } else PairContextArray[pairnumber][3]= new String[0];
        
        	sub=3;
            if (Righttokens.length != 0 ) {
            	if ((Righttokens.length == 1 ))
            	sub = 1; else if (Righttokens.length == 2 ) sub = 2;
            	PairContextArray[pairnumber][4] = Arrays.copyOfRange(Righttokens,0, sub+1);
            } else PairContextArray[pairnumber][4]=new String[0];
 

            
            
            
    /////////////pos and lemma //////////////////          
        
            
            // The sample string        
            if (pairhasrelation[pairnumber] > 0) 
            	{outwekafile.print("1,"); 
            	 outwekafile.print(pairhasrelation[pairnumber]+",");
            	}else {outwekafile.print("0,");
            	 outwekafile.print(0+",");
            	}
       outwekafile.print("'"+sentenceid+"|"+e1+"|"+e2+"',");
      	outwekafile.print("'"+LeftContext+"',");
 		outwekafile.print("'"+firstdrugname+"',");
  		outwekafile.print("'"+MiddleContext+"',");
  		outwekafile.print("'"+Seconddrugname+"',");
  		outwekafile.print("'"+RightContext+"',");
  		outwekafile.print("'"+LeftContextneg+"',");
  		outwekafile.print("'"+MiddleContextneg+"',");
  		outwekafile.print("'"+RightContextneg+"',");
 		 
        outwekafile.print("'"+Arrays.toString(PairContextArray[pairnumber][3])+"',");
  		outwekafile.print("'"+Arrays.toString(PairContextArray[pairnumber][4])+"',");
        System.setProperty("treetagger.home", "TreeTagger");  
        TreeTaggerWrapper<String> tt = new TreeTaggerWrapper<String>();   
        try {  
        	final String[] pairtemp = new String[pairstringtokens.length]; 
        	final String[] pairtemp2 = new String[pairstringtokens.length]; 
        	final String[] pairtemp3 = new String[pairstringtokens.length]; 
        	final String[] pairtemp4 = new String[pairstringtokens.length]; 
      	tt.setModel("TreeTagger/lib/english.par"); 
        	tt.setHandler(new TokenHandler<String>() { 
        		int tokennumber=0;
        		public void token(String token, String pos, String lemma) { 
     			pairtemp[tokennumber] =  token + "\t" + pos + "\t" + lemma;
     			pairtemp2[tokennumber] =  token ;
     			pairtemp3[tokennumber] =  pos;
     			pairtemp4[tokennumber] =  lemma;
   			tokennumber++;
        			}                        
        		});
   			tt.process(Arrays.asList( pairstringtokens ));
			PairContextArray[pairnumber][5] = pairtemp;
			PairContextArray[pairnumber][6] = pairtemp2;
			PairContextArray[pairnumber][7] = pairtemp3;
			PairContextArray[pairnumber][8] = pairtemp4;
   	}             
        finally {         
        	tt.destroy();               
        	}  
        int count=0;
        outwekafile.print("'");
	       for (int l11=0;l11<leftDrugTokenPosition;l11++)
	    	   outwekafile.print(PairContextArray[pairnumber][7][l11].concat(PairContextArray[pairnumber][6][l11])+ " ");  /// conj pos+token
  			    outwekafile.print("',");
	 
  		        outwekafile.print("'");
	      for (int l2=0;l2<leftDrugTokenPosition;l2++)
	    		outwekafile.print(PairContextArray[pairnumber][7][l2].concat(PairContextArray[pairnumber][8][l2])+ " ");  /// conj pos+lemma
  			    outwekafile.print("',");
	    	    	       
  		        outwekafile.print("'");
	      for (int l2=0;l2<leftDrugTokenPosition;l2++)
	    		outwekafile.print(PairContextArray[pairnumber][7][l2].concat(stem.stripAffixes(PairContextArray[pairnumber][6][l2]))+ " ");  /// conj pos+stem
  			    outwekafile.print("',");

  		        outwekafile.print("'");
  			       for (int l11=leftDrugTokenPosition;l11<rightDrugTokenPosition;l11++)
  			    
  			    	   outwekafile.print(PairContextArray[pairnumber][7][l11].concat(PairContextArray[pairnumber][6][l11])+ " ");  /// conj pos+token
  		  			    outwekafile.print("',");
  			 
  		  		        outwekafile.print("'");
  			      for (int l2=leftDrugTokenPosition;l2<rightDrugTokenPosition;l2++)
  			    		outwekafile.print(PairContextArray[pairnumber][7][l2].concat(PairContextArray[pairnumber][8][l2])+ " ");  /// conj pos+lemma
  		  			    outwekafile.print("',");
  			    	    	       
  		  		        outwekafile.print("'");
  			      for (int l2=leftDrugTokenPosition;l2<rightDrugTokenPosition;l2++)
  			    		outwekafile.print(PairContextArray[pairnumber][7][l2].concat(stem.stripAffixes(PairContextArray[pairnumber][6][l2]))+ " ");  /// conj pos+stem
  		  			    outwekafile.print("',");
 			    
 ////////////////right side
  		  		        outwekafile.print("'");
  	  			       for (int l11=rightDrugTokenPosition;l11<PairContextArray[pairnumber][5].length;l11++)
  	  			    
  	  			    	   outwekafile.print(PairContextArray[pairnumber][7][l11].concat(PairContextArray[pairnumber][6][l11])+ " ");  /// conj pos+token
  	  		  			    outwekafile.print("',");
  	  			 
  	  		  		        outwekafile.print("'");
  	  			      for (int l2=rightDrugTokenPosition;l2<PairContextArray[pairnumber][5].length;l2++)
  	  			    		outwekafile.print(PairContextArray[pairnumber][7][l2].concat(PairContextArray[pairnumber][8][l2])+ " ");  /// conj pos+lemma
  	  		  			    outwekafile.print("',");
  	  			    	    	       
  	  		  		        outwekafile.print("'");
  	  			      for (int l2=rightDrugTokenPosition;l2<PairContextArray[pairnumber][5].length;l2++)
  	  			    		outwekafile.print(PairContextArray[pairnumber][7][l2].concat(stem.stripAffixes(PairContextArray[pairnumber][6][l2]))+ " ");  /// conj pos+stem
  	  		  			    outwekafile.print("',");
  	 	///////////////
  			    outwekafile.print("'");
	      for (int l3=0;l3<PairContextArray[pairnumber][5].length;l3++)
	    		outwekafile.print(stem.stripAffixes(PairContextArray[pairnumber][6][l3])+ " ");  /// stemm
  			    outwekafile.print("',");
	    	    	 		
  		       for (int l=0;l<PairContextArray[pairnumber][5].length;l++){
  		        	
  					if (PairContextArray[pairnumber][6][l].contains(firstdrugname)&&(count==0)) 
  						{
  					outwekafile.print("'"+PairContextArray[pairnumber][6][l]+"\t") ;
  	  			    outwekafile.print("',");
  						count = 1;
  						outwekafile.print("'");
  				    for (int l1=0;l1<3;l1++)
  						if (l-l1-1 > -1) 	
  						outwekafile.print(PairContextArray[pairnumber][6][l-l1-1]+"\t");
  						outwekafile.print("',");
  						}
  			else 
  							if (PairContextArray[pairnumber][6][l].contains(Seconddrugname)&&(count==1))
  							{
  								outwekafile.print("'"+PairContextArray[pairnumber][6][l]+"\t") ;
  				  			    outwekafile.print("',");
  			  			count = 2;
  			  			outwekafile.print("'");
  				    for (int l1=0;l1<3;l1++)
  				    	  if ((l+l1+1) < (PairContextArray[pairnumber][5].length)) 	
  				    		  outwekafile.print(PairContextArray[pairnumber][6][l+l1+1]+"\t");
  		   			    outwekafile.print("',");
  							}
  			}
  			if (count==0) {outwekafile.print("'',");outwekafile.print("'',");outwekafile.print("'',");outwekafile.print("'',");} else if (count==1) {outwekafile.print("'',");outwekafile.print("'',");}
  		
	
  			
  			/// drug first  pos lemaa and three word before
  			count=0;
  			for (int l=0;l<PairContextArray[pairnumber][5].length;l++){
 		        	
					if (PairContextArray[pairnumber][6][l].contains(firstdrugname)&&(count==0)) 
						{
					outwekafile.print("'"+PairContextArray[pairnumber][7][l]+"\t") ;
	  			    outwekafile.print("',");
						count = 1;
						outwekafile.print("'");
				    for (int l1=0;l1<3;l1++)
						if (l-l1-1 > -1) 	
						outwekafile.print(PairContextArray[pairnumber][7][l-l1-1]+"\t");
						outwekafile.print("',");
						}
			else 
							if (PairContextArray[pairnumber][6][l].contains(Seconddrugname)&&(count==1))
							{
								outwekafile.print("'"+PairContextArray[pairnumber][7][l]+"\t") ;
				  			    outwekafile.print("',");
			  			count = 2;
			  			outwekafile.print("'");
				    for (int l1=0;l1<3;l1++)
				    	  if ((l+l1+1) < (PairContextArray[pairnumber][5].length)) 	
				    		  outwekafile.print(PairContextArray[pairnumber][7][l+l1+1]+"\t");
		   			    outwekafile.print("',");
							}
			}
			if (count==0) {outwekafile.print("'',");outwekafile.print("'',");outwekafile.print("'',");outwekafile.print("'',");} else if (count==1) {outwekafile.print("'',");outwekafile.print("'',");}

 			count=0;
  			for (int l=0;l<PairContextArray[pairnumber][5].length;l++){
 		        	
					if (PairContextArray[pairnumber][6][l].contains(firstdrugname)&&(count==0)) 
						{
					outwekafile.print("'"+PairContextArray[pairnumber][8][l]+"\t") ;
	  			    outwekafile.print("',");
						count = 1;
						outwekafile.print("'");
				    for (int l1=0;l1<3;l1++)
						if (l-l1-1 > -1) 	
						outwekafile.print(PairContextArray[pairnumber][8][l-l1-1]+"\t");
						outwekafile.print("',");
						}
			else 
							if (PairContextArray[pairnumber][6][l].contains(Seconddrugname)&&(count==1))
							{
								outwekafile.print("'"+PairContextArray[pairnumber][8][l]+"\t") ;
				  			    outwekafile.print("',");
			  			count = 2;
			  			outwekafile.print("'");
				    for (int l1=0;l1<3;l1++)
				    	  if ((l+l1+1) < (PairContextArray[pairnumber][5].length)) 	
				    		  outwekafile.print(PairContextArray[pairnumber][8][l+l1+1]+"\t");
		   			    outwekafile.print("',");
							}
			}
			if (count==0) {outwekafile.print("'',");outwekafile.print("'',");outwekafile.print("'',");outwekafile.print("'',");} else if (count==1) {outwekafile.print("'',");outwekafile.print("'',");}
 			
		int ll=0;
		int rr=0;
		outwekafile.print("'");  /// verbs between two drugs and after second drug and before first drug
		for(int i=0;i<PairContextArray[pairnumber][5].length; i++){
			if (PairContextArray[pairnumber][6][i].contains(firstdrugname)) ll=1; else
				if (PairContextArray[pairnumber][6][i].contains(Seconddrugname)) rr=1;
			if ((PairContextArray[pairnumber][7][i].contains("VV") || PairContextArray[pairnumber][7][i].contains("VB") ||PairContextArray[pairnumber][7][i].contains("VD")||PairContextArray[pairnumber][7][i].contains("VH")||PairContextArray[pairnumber][7][i].contains("VM")) && ((ll==1)&&(rr==0))) 
				{
				outwekafile.print(PairContextArray[pairnumber][6][i]+"\t");
				}
		}
		outwekafile.print("',");
		ll=0;rr=0;
		for(int i=0;i<PairContextArray[pairnumber][5].length; i++)
			if (PairContextArray[pairnumber][6][i].contains(firstdrugname))
			{ ll=i;break;}
		    
		    outwekafile.print("'");
			for(int i=ll-1;i>-1; i--)
			if ( (PairContextArray[pairnumber][7][i].contains("VV") || PairContextArray[pairnumber][7][i].contains("VB") ||PairContextArray[pairnumber][7][i].contains("VD")||PairContextArray[pairnumber][7][i].contains("VH")||PairContextArray[pairnumber][7][i].contains("VM")))
					{outwekafile.print(PairContextArray[pairnumber][6][i]+"\t");break;}
			outwekafile.print("',");				
			ll=0;rr=0;
			for(int i=0;i<PairContextArray[pairnumber][5].length; i++)
				if (PairContextArray[pairnumber][6][i].contains(Seconddrugname)) ll=i;
		    
			    outwekafile.print("'");
				for(int i=ll;i<PairContextArray[pairnumber][5].length; i++)
				if ( (PairContextArray[pairnumber][7][i].contains("VV") || PairContextArray[pairnumber][7][i].contains("VB") ||PairContextArray[pairnumber][7][i].contains("VD")||PairContextArray[pairnumber][7][i].contains("VH")||PairContextArray[pairnumber][7][i].contains("VM")))
				{outwekafile.print(PairContextArray[pairnumber][6][i]+"\t");break;}
				outwekafile.print("',");

				 ll=0;
				 rr=0;
				outwekafile.print("'");  /// verbs between two drugs and after second drug and before first drug
				for(int i=0;i<PairContextArray[pairnumber][5].length; i++){
					if (PairContextArray[pairnumber][6][i].contains(firstdrugname)) ll=1; else
						if (PairContextArray[pairnumber][6][i].contains(Seconddrugname)) rr=1;
					if ((PairContextArray[pairnumber][7][i].contains("VV") || PairContextArray[pairnumber][7][i].contains("VB") ||PairContextArray[pairnumber][7][i].contains("VD")||PairContextArray[pairnumber][7][i].contains("VH")||PairContextArray[pairnumber][7][i].contains("VM")) && ((ll==1)&&(rr==0))) 
						{
						outwekafile.print(PairContextArray[pairnumber][7][i]+"\t");
						}
				}
				outwekafile.print("',");
				ll=0;rr=0;
				for(int i=0;i<PairContextArray[pairnumber][5].length; i++)
					if (PairContextArray[pairnumber][6][i].contains(firstdrugname))
					{ ll=i;break;}
				    
				    outwekafile.print("'");
					for(int i=ll-1;i>-1; i--)
					if ( (PairContextArray[pairnumber][7][i].contains("VV") || PairContextArray[pairnumber][7][i].contains("VB") ||PairContextArray[pairnumber][7][i].contains("VD")||PairContextArray[pairnumber][7][i].contains("VH")||PairContextArray[pairnumber][7][i].contains("VM")))
							{outwekafile.print(PairContextArray[pairnumber][7][i]+"\t");break;}
					outwekafile.print("',");				
					ll=0;rr=0;
					for(int i=0;i<PairContextArray[pairnumber][5].length; i++)
						if (PairContextArray[pairnumber][6][i].contains(Seconddrugname)) ll=i;
				    
					    outwekafile.print("'");
						for(int i=ll;i<PairContextArray[pairnumber][5].length; i++)
						if ( (PairContextArray[pairnumber][7][i].contains("VV") || PairContextArray[pairnumber][7][i].contains("VB") ||PairContextArray[pairnumber][7][i].contains("VD")||PairContextArray[pairnumber][7][i].contains("VH")||PairContextArray[pairnumber][7][i].contains("VM")))
						{outwekafile.print(PairContextArray[pairnumber][7][i]+"\t");break;}
						outwekafile.print("',");

						
						 ll=0;
						 rr=0;
						outwekafile.print("'");  /// verbs between two drugs and after second drug and before first drug
						for(int i=0;i<PairContextArray[pairnumber][5].length; i++){
							if (PairContextArray[pairnumber][6][i].contains(firstdrugname)) ll=1; else
								if (PairContextArray[pairnumber][6][i].contains(Seconddrugname)) rr=1;
							if ((PairContextArray[pairnumber][7][i].contains("VV") || PairContextArray[pairnumber][7][i].contains("VB") ||PairContextArray[pairnumber][7][i].contains("VD")||PairContextArray[pairnumber][7][i].contains("VH")||PairContextArray[pairnumber][7][i].contains("VM")) && ((ll==1)&&(rr==0))) 
								{
								outwekafile.print(PairContextArray[pairnumber][8][i]+"\t");
								}
						}
						outwekafile.print("',");
						ll=0;rr=0;
						for(int i=0;i<PairContextArray[pairnumber][5].length; i++)
							if (PairContextArray[pairnumber][6][i].contains(firstdrugname))
							{ ll=i;break;}
						    
						    outwekafile.print("'");
							for(int i=ll-1;i>-1; i--)
							if ( (PairContextArray[pairnumber][7][i].contains("VV") || PairContextArray[pairnumber][7][i].contains("VB") ||PairContextArray[pairnumber][7][i].contains("VD")||PairContextArray[pairnumber][7][i].contains("VH")||PairContextArray[pairnumber][7][i].contains("VM")))
									{outwekafile.print(PairContextArray[pairnumber][8][i]+"\t");break;}
							outwekafile.print("',");				
							ll=0;rr=0;
							for(int i=0;i<PairContextArray[pairnumber][5].length; i++)
								if (PairContextArray[pairnumber][6][i].contains(Seconddrugname)) ll=i;
						    
							    outwekafile.print("'");
								for(int i=ll;i<PairContextArray[pairnumber][5].length; i++)
								if ( (PairContextArray[pairnumber][7][i].contains("VV") || PairContextArray[pairnumber][7][i].contains("VB") ||PairContextArray[pairnumber][7][i].contains("VD")||PairContextArray[pairnumber][7][i].contains("VH")||PairContextArray[pairnumber][7][i].contains("VM")))
								{outwekafile.print(PairContextArray[pairnumber][8][i]+"\t");break;}
								outwekafile.print("',");

//////////////////numeric features //////////////////				
/*				outwekafile.print(LeftContext.length()+",");
				outwekafile.print(MiddleContext.length()+",");
				outwekafile.print(RightContext.length()+",");
				outwekafile.print(leftDrugTokenPosition+",");
				outwekafile.print(rightDrugTokenPosition+",");*/

/////     tree features  down
			 		lp.parse(pairstringWord);     
					int firstindex1 = -1;
					int secondindex1 = -1;
		//	 		Tree tb = lp.getBestParse(); // get the best parse tree
			 		Tree t =lp.getBestPCFGParse(false);
			 	//	Tree t = lp.getBestDependencyParse(false);
					for (int j1=0;j1 < t.getLeaves().size();j1++){
						String FirstEntitytext="DrugNameF";
						String SecondEntitytext="DrugNameS";
						if ((t.getLeaves().get(j1).toString().equalsIgnoreCase(FirstEntitytext))&& (firstindex1 == -1))
							firstindex1=j1;
						if ((t.getLeaves().get(j1).toString().equalsIgnoreCase(SecondEntitytext))&& (firstindex1>-1))
							secondindex1=j1;
					}
					int size1=0;
					String SsubSetTree = "";
					if ((firstindex1 > -1)& (secondindex1 >-1)){
				  size1 = t.pathNodeToNode(t.getLeaves().get(firstindex1), t.getLeaves().get(secondindex1)).size();
					List<Tree> path1 = t.pathNodeToNode(t.getLeaves().get(firstindex1), t.getLeaves().get(secondindex1));
					List<Tree> path2 = t.pathNodeToNode(t.getLeaves().get(0), t.getLeaves().get(firstindex1));
					List<Tree> path3 = t.pathNodeToNode(t.getLeaves().get(secondindex1), t.getLeaves().get(t.getLeaves().size()-1));
				    SsubSetTree = subSetTree(t,firstindex1,secondindex1);
					outwekafile.print("'"+path1+"',");
					outwekafile.print("'"+path2+"',");
					outwekafile.print("'"+path3+"',");
					} else {
						outwekafile.print("'',");
						outwekafile.print("'',");
						outwekafile.print("'',");
						
					}
				
					outwekafile.print("'"+SsubSetTree+"',");
					
	/*				for (int j1=0;j1 < tb.getLeaves().size();j1++){
						String FirstEntitytext="DrugNameF";
						String SecondEntitytext="DrugNameS";
						if ((tb.getLeaves().get(j1).toString().equalsIgnoreCase(FirstEntitytext))&& (firstindex1 == -1))
							firstindex1=j1;
						if ((tb.getLeaves().get(j1).toString().equalsIgnoreCase(SecondEntitytext))&& (firstindex1>-1))
							secondindex1=j1;
					}
					 size1=0;
					if ((firstindex1 > -1)& (secondindex1 >-1)){
				  size1 = tb.pathNodeToNode(tb.getLeaves().get(firstindex1), tb.getLeaves().get(secondindex1)).size();
					List<Tree> path1 = tb.pathNodeToNode(tb.getLeaves().get(firstindex1), tb.getLeaves().get(secondindex1));
					List<Tree> path2 = tb.pathNodeToNode(tb.getLeaves().get(0), t.getLeaves().get(firstindex1));
					List<Tree> path3 = t.pathNodeToNode(tb.getLeaves().get(secondindex1), tb.getLeaves().get(tb.getLeaves().size()-1));
					outwekafile.print("'"+path1+"',");
					outwekafile.print("'"+path2+"',");
					outwekafile.print("'"+path3+"',");
					} else {
						outwekafile.print("'',");
						outwekafile.print("'',");
						outwekafile.print("'',");
						
					}*/
//					outwekafile.print(size1+"',"); 
////////  tree features  up

			
					outwekafile.println("'"+files+"'");

			}  
			String sentence1 = elSentence.getAttribute("text");
			outPairs.println(sentence1);  // fill the output file Pairs
			outPairs.close();

		      }  
       		} 
		}
      numSections = numSections + doc.getElementsByTagName("sentence").getLength();
		}  
		  }  
	
	 	outwekafile.close();

} 
}
		  
	
	
	


