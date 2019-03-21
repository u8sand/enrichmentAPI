package main.java.serv;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import main.java.jsp.Result;
import main.java.math.FastFisher;

/**
 * Servlet implementation class Test
 */
@WebServlet("/api/v1/*")
public class EnrichmentTemp extends HttpServlet {
	private static final long serialVersionUID = 1L;
    public FastFisher f;
	
	public boolean initialized = false;
	
	Enrichment enrich = null;
	
	public HashMap<String, HashMap<String, String>> genemap;
	public HashMap<String, HashMap<String, String>> genemaprev;
	public HashSet<String> humanGenesymbol = new HashSet<String>();
	public HashSet<String> mouseGenesymbol = new HashSet<String>();
	
	public HashMap<String, Integer> symbolToId = new HashMap<String, Integer>();
	public HashMap<Integer, String> idToSymbol = new HashMap<Integer, String>();
	
	
	public Connection connection;
	
	
    /**
     * @see HttpServlet#HttpServlet()
     */
    public EnrichmentTemp() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see Servlet#init(ServletConfig)
	 */
	public void init(ServletConfig config) throws ServletException {
		
		super.init(config);
		
		// TODO Auto-generated method stub
		f = new FastFisher(40000);
		
		
		try {
			
			System.out.println("Start buffering datasets");
			enrich = new Enrichment();
			System.out.println("... and ready!");

		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		String pathInfo = request.getPathInfo();
		System.out.println(pathInfo);
		response.setHeader("Content-Type", "application/json");
		
		if(pathInfo == null || pathInfo.equals("/index.html") || pathInfo.equals("/")){
			RequestDispatcher rd = getServletContext().getRequestDispatcher("/index.html");
			PrintWriter out = response.getWriter();
			out.write("index.html URL");
			rd.include(request, response);
		}
		else if(pathInfo.matches("^/listdata")){
			//localhost:8080/EnrichmentAPI/enrichment/listcategories
			PrintWriter out = response.getWriter();
			

			StringBuffer sb = new StringBuffer();
			
			sb.append("{ \"repositories\": [");
			
			for(String db : enrich.datastore.datasets.keySet()){
				sb.append("{\"uuid\": \"").append(db).append("\", \"datatype\":\"").append(enrich.datastore.datasets.get(db).getDatasetType()).append("\"},");
			}
			sb.append("]}");
			
			String json = sb.toString();
			json = json.replace(",]", "]");
			out.write(json);
		}
		else if(pathInfo.matches("^/enrich/overlap/.*")) {
			
			long  time = System.currentTimeMillis();
			String truncPathInfo = pathInfo.replace("/enrich/overlap", "");
			
			Pattern p = Pattern.compile("/db/(.*)/entities/(.*)/signatures/(.*)");
		    Matcher m = p.matcher(truncPathInfo);
		    
		    String[] entity_split = new String[0];
		    String db = "";
		    HashSet<String> signatures = new HashSet<String>();
		    boolean queryValid = false;
		    
		    // if our pattern matches the URL extract groups
		    if (m.find()){
		    	db = m.group(1);
		    	entity_split = m.group(2).split(",");
		    	signatures = new HashSet<String>(Arrays.asList(m.group(3).split(",")));
		    	queryValid = true;
		    }
		    else{	// enrichment over all geneset libraries
		    	p = Pattern.compile("/db/(.*)/entities/(.*)");
			    m = p.matcher(truncPathInfo);
			    
			    if(m.find()){
			    	db = m.group(1);
			    	entity_split = m.group(2).split(",");
			    	queryValid = true;
			    }
			    else {
			    	System.out.println("API endpoint unknown.");
			    }
		    }
		    
		    if(queryValid) {
				if(enrich.datastore.datasets.get(db).getData().containsKey("geneset")) {
					// The database is a gene set collection
					
					// filter signature and entities that match the database 
					HashSet<String> entities = new HashSet<String>(Arrays.asList(entity_split));
					HashMap<String, Short> dict = (HashMap<String, Short>) enrich.datastore.datasets.get(db).getData().get("dictionary");
					HashSet<String> dictEntities = new HashSet<String>(dict.keySet());
					entities.retainAll(dictEntities);
					
					HashSet<String> sigs = new HashSet<String>(((HashMap<String, Short>) enrich.datastore.datasets.get(db).getData().get("geneset")).keySet());
					signatures.retainAll(sigs);
					
					HashMap<String, Result> enrichResult = enrich.calculateOverlapEnrichment(db, entities.toArray(new String[0]), signatures, 0.5);
					returnOverlapJSON(response, enrichResult, db, signatures, entities, time, 0, 1000);
				}
		    }
		}
		else if(pathInfo.matches("^/enrich/rank/.*")){
			long  time = System.currentTimeMillis();
			String truncPathInfo = pathInfo.replace("/enrich/rank", "");
			
			Pattern p = Pattern.compile("/db/(.*)/entities/(.*)/signatures/(.*)");
		    Matcher m = p.matcher(truncPathInfo);
		    
		    String[] entity_split = new String[0];
		    String db = "";
		    HashSet<String> signatures = new HashSet<String>();
		    boolean queryValid = false;
		    
		    // if our pattern matches the URL extract groups
		    if (m.find()){
		    	db = m.group(1);
		    	entity_split = m.group(2).split(",");
		    	signatures = new HashSet<String>(Arrays.asList(m.group(3).split(",")));
		    	queryValid = true;
		    }
		    else{	// enrichment over all geneset libraries
		    	p = Pattern.compile("/db/(.*)/entities/(.*)");
			    m = p.matcher(truncPathInfo);
			    
			    if(m.find()){
			    	db = m.group(1);
			    	entity_split = m.group(2).split(",");
			    	queryValid = true;
			    }
			    else {
			    	System.out.println("API endpoint unknown.");
			    }
		    }
		    
		    if(queryValid) {
		    	if(enrich.datastore.datasets.get(db).getData().containsKey(db)) {
					if(enrich.datastore.datasets.get(db).getData().containsKey("rank")) {
						// The database is a gene set collection
						
						// filter signature and entities that match the database 
						
						HashSet<String > entities = new HashSet<String>(Arrays.asList(entity_split));
						entities.retainAll(Arrays.asList(((String[]) enrich.datastore.datasets.get(db).getData().get("entity_id"))));
						signatures.retainAll(Arrays.asList(((String[]) enrich.datastore.datasets.get(db).getData().get("signature_id"))));
						
						HashMap<String, Result> enrichResult = enrich.calculateRankEnrichment(db, entities.toArray(new String[0]), signatures, 0.05);
						returnRankJSON(response, enrichResult, db, signatures, entities, time, 0, 1000);
					}
			    }
		    }
		}	
	}
	
	private void returnOverlapJSON(HttpServletResponse _response, HashMap<String, Result> _result, String _db, HashSet<String> _signatures,  HashSet<String> _entities, long _time, int _offset, int _limit) {
		try {
			_response.addHeader("Content-Type", "application/json");
			_response.addHeader("Access-Control-Allow-Origin", "*");
			_response.addHeader("Access-Control-Expose-Headers", "Content-Range,X-Duration");
			
			
			PrintWriter out = _response.getWriter();
			
			HashMap<String, Result> enrichResult = _result;
			HashMap<Short, String> revdict = (HashMap<Short, String>) enrich.datastore.datasets.get(_db).getData().get("revDictionary");
			
			Result[] resultArray = new Result[enrichResult.size()];
			int counter = 0;
			for(String key : enrichResult.keySet()){
				resultArray[counter] = enrichResult.get(key);
				counter++;
			}
			
			Arrays.sort(resultArray);
			
			System.out.println(_offset +" - "+_limit);
			
			StringBuffer sb = new StringBuffer();
			sb.append("{");
			
			sb.append("\"signatures\" : [");
			for(String ui : _signatures){
				sb.append("\"").append(ui).append("\", ");	
			}
			sb.append("], ");
			
			sb.append("\"matchingEntities\" : [");
			for(String match : _entities){
				sb.append("\"").append(match).append("\", ");	
			}
			
			sb.append("], \"queryTimeSec\": ").append(((System.currentTimeMillis()*1.0 - _time)/1000)).append(", \"results\": [");
			
			_response.addHeader("X-Duration", ""+(System.currentTimeMillis()*1.0 - _time)/1000);
			_offset = Math.min(Math.max(0, _offset), resultArray.length-1);
			_limit = Math.min(_offset+Math.max(1, _limit), resultArray.length);
			_response.addHeader("Content-Range", ""+_offset+"-"+_limit+"/"+resultArray.length);
			
			for(int i=_offset; i<_limit; i++){
				Result res = resultArray[i];
				String genesetName = res.name;
				double pval = res.pval;
				short[] overlap = res.overlap;
				double oddsratio = res.oddsRatio;
				int setsize = res.setsize;	
				
				sb.append("{");
				sb.append("\"uuid\" : \"").append(genesetName).append("\", ");
				sb.append("\"p-value\" : ").append(pval).append(", ");
				sb.append("\"oddsratio\" : ").append(oddsratio).append(", ");
				sb.append("\"setsize\" : ").append(setsize).append(", ");
				sb.append("\"overlap\" : [");
				
				for(short overgene : overlap){
					sb.append("\"").append(revdict.get((Short)overgene)).append("\", ");	
				}
				sb.append("]}, ");
			}
			
			sb.append("]}");
			String json = sb.toString();
			json = json.replace(", }", "}");
			json = json.replace(", ]", "]");
			
			System.out.println(json);
			
			out.write(json);
			
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private void returnRankJSON(HttpServletResponse _response, HashMap<String, Result> _result, String _db, HashSet<String> _signatures,  HashSet<String> _entities, long _time, int _offset, int _limit) {
		try {
			_response.addHeader("Content-Type", "application/json");
			_response.addHeader("Access-Control-Allow-Origin", "*");
			_response.addHeader("Access-Control-Expose-Headers", "Content-Range,X-Duration");
			
			
			PrintWriter out = _response.getWriter();
			
			HashMap<String, Result> enrichResult = _result;
			
			Result[] resultArray = new Result[enrichResult.size()];
			int counter = 0;
			for(String key : enrichResult.keySet()){
				resultArray[counter] = enrichResult.get(key);
				counter++;
			}
			
			Arrays.sort(resultArray);
			
			StringBuffer sb = new StringBuffer();
			sb.append("{");
			
			sb.append("\"signatures\" : [");
			for(String ui : _signatures){
				sb.append("\"").append(ui).append("\", ");	
			}
			sb.append("], ");
			
			sb.append("\"queryTimeSec\": ").append(((System.currentTimeMillis()*1.0 - _time)/1000)).append(", \"results\": [");
			
			_offset = Math.min(Math.max(0, _offset), resultArray.length-1);
			_limit = Math.max(1, _limit);
			

			_response.addHeader("X-Duration", ""+(System.currentTimeMillis()*1.0 - _time)/1000);
			_offset = Math.min(Math.max(0, _offset), resultArray.length-1);
			_limit = Math.min(_offset+Math.max(1, _limit), resultArray.length);
			_response.addHeader("Content-Range", ""+_offset+"-"+_limit+"/"+resultArray.length);
			
			for(int i=_offset; i<_limit; i++){
				Result res = resultArray[i];
				String signature = res.name;
				if(signature != null) {
					String genesetName = signature;
					double pval = enrichResult.get(signature).pval;
					
					sb.append("{\"uuid\":\"").append(genesetName).append("\", \"p-value\":").append(pval).append(", \"zscore\":").append(enrichResult.get(signature).zscore).append(", \"direction\":").append(enrichResult.get(signature).direction).append("}, ");
				}
			}
			sb.append("]}");
			
			String json = sb.toString();
			json = json.replace(", }", "}");
			json = json.replace(", ]", "]");
			out.write(json);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private void returnRankTwoWayJSON(HttpServletResponse _response, HashMap<String, Result> _resultUp, HashMap<String, Result> _resultDown, String _db, HashSet<String> _signatures, HashSet<String> _entities, long _time, int _offset, int _limit) {
		try {
			_response.addHeader("Content-Type", "application/json");
			_response.addHeader("Access-Control-Allow-Origin", "*");
			_response.addHeader("Access-Control-Expose-Headers", "Content-Range,X-Duration");
			
			PrintWriter out = _response.getWriter();
			
			HashMap<String, Result> enrichResultUp = _resultUp;
			HashMap<String, Result> enrichResultDown = _resultDown;
			HashMap<String, Double> enrichResultFisher = new HashMap<String, Double>();
			HashMap<String, Double> enrichResultAvg = new HashMap<String, Double>();
			
			String[] keys = enrichResultUp.keySet().toArray(new String[0]);
			
			double[] pvalsUp = new double[keys.length];
			double[] pvalsDown = new double[keys.length];
			
			for(int i=0; i<keys.length; i++) {
				pvalsUp[i] = enrichResultUp.get(keys[i]).pval;
				pvalsDown[i] = enrichResultDown.get(keys[i]).pval;
				
				enrichResultFisher.put(keys[i], Math.abs((enrichResultUp.get(keys[i]).zscore*enrichResultDown.get(keys[i]).zscore)));
				enrichResultAvg.put(keys[i], Math.abs((enrichResultUp.get(keys[i]).zscore)+Math.abs(enrichResultDown.get(keys[i]).zscore)));
			}
			
			System.out.println("Signatures: "+_signatures.size());
			System.out.println("Result count: " + _resultUp.size());
			
			Map<String, Double> sortedFisher = sortByValues((Map<String,Double>)enrichResultFisher, 1);
			String[] sortFish = new String[sortedFisher.size()];
			
			int counter = 0;
			for (Map.Entry<String, Double> me : sortedFisher.entrySet()) { 
				sortFish[counter] = me.getKey();
			    counter++;
			} 
			
			StringBuffer sb = new StringBuffer();
			sb.append("{");
			
			sb.append("\"signatures\" : [");
			for(String ui : _signatures){
				sb.append("\"").append(ui).append("\", ");	
			}
			sb.append("], ");
			
			sb.append("\"queryTimeSec\": ").append(((System.currentTimeMillis()*1.0 - _time)/1000)).append(", \"results\": [");
			
			_response.addHeader("X-Duration", ""+(System.currentTimeMillis()*1.0 - _time)/1000);
			_offset = Math.min(Math.max(0, _offset), sortFish.length-1);
			_limit = Math.min(_offset+Math.max(1, _limit), sortFish.length);
			_response.addHeader("Content-Range", ""+_offset+"-"+_limit+"/"+sortFish.length);
			
			for(int i=_offset; i<_limit; i++){
				String signature = sortFish[i];
				
				if(signature != null) {
					String genesetName = signature;
					double pvalUp = enrichResultUp.get(signature).pval;
					double pvalDown = enrichResultDown.get(signature).pval;
					double zUp = enrichResultUp.get(signature).zscore;
					double zDown = enrichResultDown.get(signature).zscore;
					double pvalFisher = enrichResultFisher.get(signature);
					double pvalSum = enrichResultAvg.get(signature);
					int direction_up = enrichResultUp.get(signature).direction;
					int direction_down = enrichResultDown.get(signature).direction;
					
					sb.append("{\"uuid\":\"").append(genesetName)
						.append("\", \"p-up\":").append(pvalUp)
						.append(", \"p-down\":").append(pvalDown)
						.append(", \"z-up\":").append(zUp)
						.append(", \"z-down\":").append(zDown)
						.append(", \"logp-fisher\":").append(pvalFisher)
						.append(", \"logp-avg\":").append(pvalSum)
						.append(", \"direction-up\":").append(direction_up)
						.append(", \"direction-down\":").append(direction_down)
						.append("}, ");
					
				}
			}
			sb.append("]}");
			
			String json = sb.toString();
			json = json.replace(", }", "}");
			json = json.replace(", ]", "]");
			out.write(json);
			
			System.out.println("data sent");
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String pathInfo = request.getPathInfo();
		System.out.println(pathInfo);
		
		response.addHeader("Content-Type", "application/json");
		response.addHeader("Access-Control-Allow-Origin", "*");
		
		if(pathInfo.matches("^/enrich/rank")){
			
			long  time = System.currentTimeMillis();
			
			StringBuffer jb = new StringBuffer();
			String line = null;
			try {
				BufferedReader reader = request.getReader();
				while ((line = reader.readLine()) != null)
					jb.append(line);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			String queryjson = jb.toString();
			HashSet<String> signatures = new HashSet<String>();
			ArrayList<String> entity_split = new ArrayList<String>();
			
			String db = "";
			
			int offset = 0;
			int limit = 1000;
			double significance = 0.05;
			
			try {
				final JSONObject obj = new JSONObject(queryjson);
			    
				db = (String) obj.get("database");
				
				final JSONArray queryEntities = obj.getJSONArray("entities");
			    int n = queryEntities.length();
			    
			    for (int i = 0; i < n; ++i) {
			    	entity_split.add(queryEntities.getString(i));
			    }
			    
			    if(obj.optJSONArray("signatures") != null) {
				    final JSONArray querySignatures = obj.getJSONArray("signatures");
				    n = querySignatures.length();
				    
				    for (int i = 0; i < n; ++i) {
				    	signatures.add(querySignatures.getString(i));
				    }
			    }
			    
			    if(obj.opt("offset") != null) {
			    	offset = (int) obj.get("offset");
			    }
			    
			    if(obj.opt("limit") != null) {
			    	limit = (int) obj.get("limit");
			    }
			    
			    System.out.println("OL: "+offset+" - "+limit);
			    
			    if(obj.opt("significance") != null) {
			    	significance = (double) obj.get("significance");
			    }
			}
		    catch(Exception e) {
		    	e.printStackTrace();
		    	
		    	PrintWriter out = response.getWriter();
				
				String json = "{\"error\": \"malformed JSON query data\", \"endpoint:\" : \""+pathInfo+"\"}";
				out.write(json);
		    }
			
			System.out.println(db);
			if(enrich.datastore.datasets.get(db).getData().containsKey("rank")) {
				// The database is a gene set collection	
				HashSet<String > entities = new HashSet<String>(entity_split);
				entities.retainAll(Arrays.asList(((String[]) enrich.datastore.datasets.get(db).getData().get("entity_id"))));
				signatures.retainAll(Arrays.asList(((String[]) enrich.datastore.datasets.get(db).getData().get("signature_id"))));
				
				System.out.println(entities.size()+" - "+signatures.size());
				
				HashMap<String, Result> enrichResult = enrich.calculateRankEnrichment(db, entities.toArray(new String[0]), signatures, significance);
				System.out.println("ER: "+enrichResult.size());
				returnRankJSON(response, enrichResult, db, signatures, entities, time, offset, limit);
			}
		}
		else if(pathInfo.matches("^/enrich/ranktwosided")){
			
			long  time = System.currentTimeMillis();
			
			StringBuffer jb = new StringBuffer();
			String line = null;
			try {
				BufferedReader reader = request.getReader();
				while ((line = reader.readLine()) != null)
					jb.append(line);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			String queryjson = jb.toString();
			HashSet<String> signatures = new HashSet<String>();
			ArrayList<String> entity_split_up = new ArrayList<String>();
			ArrayList<String> entity_split_down = new ArrayList<String>();
			
			String db = "";
			
			int offset = 0;
			int limit = 0;
			double significance = 0.05;
			
			try {
				final JSONObject obj = new JSONObject(queryjson);
			    
				db = (String) obj.get("database");
				
				JSONArray queryEntities = obj.getJSONArray("up_entities");
			    int n = queryEntities.length();
			    
			    for (int i = 0; i < n; ++i) {
			    	entity_split_up.add(queryEntities.getString(i));
			    }
			    
			    queryEntities = obj.getJSONArray("down_entities");
			    n = queryEntities.length();
			    
			    for (int i = 0; i < n; ++i) {
			    	entity_split_down.add(queryEntities.getString(i));
			    }
			    
			    if(obj.optJSONArray("signatures") != null) {
				    final JSONArray querySignatures = obj.getJSONArray("signatures");
				    n = querySignatures.length();
				    
				    for (int i = 0; i < n; ++i) {
				    	signatures.add(querySignatures.getString(i));
				    }
			    }
			    
			    if(obj.opt("offset") != null) {
			    	offset = (int) obj.get("offset");
			    }
			    
			    
			    if(obj.opt("limit") != null) {
			    	limit = (int) obj.get("limit");
			    }
			    
			    System.out.println("OL: "+offset+" - "+limit);
			    
			    if(obj.opt("significance") != null) {
			    	significance = (double) obj.get("significance");
			    }
			}
		    catch(Exception e) {
		    	e.printStackTrace();
		    	
		    	PrintWriter out = response.getWriter();
				
				String json = "{\"error\": \"malformed JSON query data\", \"endpoint:\" : \""+pathInfo+"\"}";
				out.write(json);
		    }
			
			System.out.println(db);
			
			if(enrich.datastore.datasets.get(db).getData().containsKey("rank")) {
				// The database is a gene set collection	

				HashSet<String > entities = new HashSet<String>(entity_split_up);
				entities.retainAll(Arrays.asList(((String[]) enrich.datastore.datasets.get(db).getData().get("entity_id"))));
				signatures.retainAll(Arrays.asList(((String[]) enrich.datastore.datasets.get(db).getData().get("signature_id"))));
				
				HashMap<String, Result> enrichResultUp = enrich.calculateRankEnrichment(db, entities.toArray(new String[0]), signatures, significance);
				
				entities = new HashSet<String>(entity_split_down);
				entities.retainAll(Arrays.asList(((String[]) enrich.datastore.datasets.get(db).getData().get("entity_id"))));
				HashMap<String, Result> enrichResultDown = enrich.calculateRankEnrichment(db, entities.toArray(new String[0]), signatures, significance);
				
				HashSet<String> unionSignificant = new HashSet<String>(enrichResultDown.keySet());
				unionSignificant.removeAll(enrichResultUp.keySet());
				entities = new HashSet<String>(entity_split_up);
				entities.retainAll(Arrays.asList(((String[]) enrich.datastore.datasets.get(db).getData().get("entity_id"))));
				
				if(unionSignificant.size() > 0) {
					HashMap<String, Result> enrichResultUp2 = enrich.calculateRankEnrichment(db, entities.toArray(new String[0]), unionSignificant, significance);
					enrichResultUp.putAll(enrichResultUp2);
				}
			
				unionSignificant = new HashSet<String>(enrichResultUp.keySet());
				unionSignificant.removeAll(enrichResultDown.keySet());
				
				if(unionSignificant.size() > 0) {
					entities = new HashSet<String>(entity_split_down);
					entities.retainAll(Arrays.asList(((String[]) enrich.datastore.datasets.get(db).getData().get("entity_id"))));
					HashMap<String, Result> enrichResultDown2 = enrich.calculateRankEnrichment(db, entities.toArray(new String[0]), unionSignificant, significance);
					enrichResultDown.putAll(enrichResultDown2);
				}
				
				returnRankTwoWayJSON(response, enrichResultUp, enrichResultDown, db, signatures, entities, time, offset, limit);
			}
		}
		else if(pathInfo.matches("^/enrich/overlap")){
			
			long  time = System.currentTimeMillis();
			
			StringBuffer jb = new StringBuffer();
			String line = null;
			try {
				BufferedReader reader = request.getReader();
				while ((line = reader.readLine()) != null)
					jb.append(line);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			String queryjson = jb.toString();
			HashSet<String> signatures = new HashSet<String>();
			ArrayList<String> entity_split = new ArrayList<String>();
			
			String db = "";
			int offset = 0;
			int limit = 1000;
			double significance = 0.05;
			
			try {
				final JSONObject obj = new JSONObject(queryjson);
			    
				db = (String) obj.get("database");
				
				final JSONArray queryEntities = obj.getJSONArray("entities");
			    int n = queryEntities.length();
			    
			    for (int i = 0; i < n; ++i) {
			    	entity_split.add(queryEntities.getString(i));
			    }
			    
			    if(obj.optJSONArray("signatures") != null) {
				    final JSONArray querySignatures = obj.getJSONArray("signatures");
				    n = querySignatures.length();
				    
				    for (int i = 0; i < n; ++i) {
				    	signatures.add(querySignatures.getString(i));
				    }
			    }
			    
			    if(obj.opt("offset") != null) {
			    	offset = (int) obj.get("offset");
			    }
			    
			    
			    if(obj.opt("limit") != null) {
			    	limit = (int) obj.get("limit");
			    }
			    
			    System.out.println("OL: "+offset+" - "+limit);
			    
			    if(obj.opt("significance") != null) {
			    	significance = (double) obj.get("significance");
			    }
			}
		    catch(Exception e) {
		    	e.printStackTrace();
		    	
		    	PrintWriter out = response.getWriter();
				
				String json = "{\"error\": \"malformed JSON query data\", \"endpoint:\" : \""+pathInfo+"\"}";
				out.write(json);
		    }
			
			if(enrich.datastore.datasets.get(db).getData().containsKey("geneset")) {
				// The database is a gene set collection	
				
				HashSet<String> entities = new HashSet<String>(entity_split);
				HashMap<String, Short> dict = (HashMap<String, Short>) enrich.datastore.datasets.get(db).getData().get("dictionary");
				HashSet<String> dictEntities = new HashSet<String>(dict.keySet());
				entities.retainAll(dictEntities);
				
				HashSet<String> sigs = new HashSet<String>(((HashMap<String, Short>) enrich.datastore.datasets.get(db).getData().get("geneset")).keySet());
				signatures.retainAll(sigs);
				
				HashMap<String, Result> enrichResult = enrich.calculateOverlapEnrichment(db, entities.toArray(new String[0]), signatures, significance);
				
				returnOverlapJSON(response, enrichResult, db, signatures, entities, time, offset, limit);
			}
		}
		else if(pathInfo.matches("^/fetch/set")){
			
			StringBuffer jb = new StringBuffer();
			String line = null;
			try {
				BufferedReader reader = request.getReader();
				while ((line = reader.readLine()) != null)
					jb.append(line);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			String queryjson = jb.toString();
			HashSet<String> signatures = new HashSet<String>();
			
			String db = "";
			
			try {
				final JSONObject obj = new JSONObject(queryjson);
			    
				db = (String) obj.get("database");
			    

			    final JSONArray querySignatures = obj.getJSONArray("signatures");
			    int n = querySignatures.length();
			    
			    for (int i = 0; i < n; ++i) {
			    	signatures.add(querySignatures.getString(i));
			    }
			}
		    catch(Exception e) {
		    	e.printStackTrace();
		    
		    	PrintWriter out = response.getWriter();
				
				String json = "{\"error\": \"malformed JSON query data\", \"endpoint:\" : \""+pathInfo+"\"}";
				out.write(json);
		    }
			
			HashMap<String, String[]> res =  enrich.getSetData(db, signatures.toArray(new String[0]));
			returnSetData(response, res);
		}
		else if(pathInfo.matches("^/fetch/rank")){
			
			StringBuffer jb = new StringBuffer();
			String line = null;
			try {
				BufferedReader reader = request.getReader();
				while ((line = reader.readLine()) != null)
					jb.append(line);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			String queryjson = jb.toString();
			HashSet<String> signatures = new HashSet<String>();
			ArrayList<String> entity_split = new ArrayList<String>();
			
			String db = "";
			
			try {
				final JSONObject obj = new JSONObject(queryjson);
			    
				db = (String) obj.get("database");
				
				if(obj.optJSONArray("entities") != null) {
					final JSONArray queryEntities = obj.getJSONArray("entities");
				    int n = queryEntities.length();
				    
				    for (int i = 0; i < n; ++i) {
				    	entity_split.add(queryEntities.getString(i));
				    }
				}
				
			    final JSONArray querySignatures = obj.getJSONArray("signatures");
			    int n = querySignatures.length();
			    
			    for (int i = 0; i < n; ++i) {
			    	signatures.add(querySignatures.getString(i));
			    }
			    
			}
		    catch(Exception e) {
		    	e.printStackTrace();
		    	
		    	PrintWriter out = response.getWriter();
				
				String json = "{\"error\": \"malformed JSON query data\", \"endpoint:\" : \""+pathInfo+"\"}";
				out.write(json);
		    }
			
			HashMap<String, Object> res =  enrich.getRankData(db, signatures.toArray(new String[0]), entity_split.toArray(new String[0]));
			returnRankData(response, res);
		}
		else if(pathInfo.matches("^/loadrepositories")){
			enrich.reloadRepositories();
			try {
				PrintWriter out = response.getWriter();
				String json = "{\"status\": \"Repositories loaded into memory. Data API ready to go.\"}";
				out.write(json);
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}
		else if(pathInfo.matches("^/listdata")){
			//localhost:8080/EnrichmentAPI/enrichment/listcategories
			PrintWriter out = response.getWriter();
			StringBuffer sb = new StringBuffer();
			
			sb.append("{ \"repositories\": [");
			
			for(String db : enrich.datastore.datasets.keySet()){
				sb.append("{\"uuid\": \"").append(db).append("\", \"datatype\":\"").append(enrich.datastore.datasets.get(db).getDatasetType()).append("\"},");
			}
			sb.append("]}");
			
			String json = sb.toString();
			json = json.replace(",]", "]");
			out.write(json);
		}
	}
	
	private void returnSetData(HttpServletResponse _response, HashMap<String, String[]> _sets) {
		
		try {
			_response.addHeader("Content-Type", "application/json");
			_response.addHeader("Access-Control-Allow-Origin", "*");
			PrintWriter out = _response.getWriter();
			
			StringBuffer sb = new StringBuffer();
			sb.append("{");
			
			sb.append("\"signatures\" : [ ");
			for(String ui : _sets.keySet()){
				sb.append("{\"uid\" : \"").append(ui).append("\", \"entities\" : [\"").append(String.join("\",\"", _sets.get(ui))).append("\"]}, ");
			}
			sb.append("]}");
			
			String json = sb.toString();
			json = json.replace(", }", "}");
			json = json.replace(", ]", "]");
			out.write(json);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private void returnRankData(HttpServletResponse _response, HashMap<String, Object> _ranks) {
		
		try {
			_response.addHeader("Content-Type", "application/json");
			_response.addHeader("Access-Control-Allow-Origin", "*");
			
			PrintWriter out = _response.getWriter();
			
			Integer maxRank = (Integer)_ranks.get("maxRank");
			HashMap<String, short[]> ranks = (HashMap<String, short[]>) _ranks.get("signatureRanks");
			String[] signatures = ranks.keySet().toArray(new String[0]);
			String[] entities = (String[]) _ranks.get("entities");
			
			StringBuffer sb = new StringBuffer();
			
			sb.append("{\"entities\" : [\"").append(String.join("\",\"", entities)).append("\"], \"maxrank\" : ").append(maxRank);
			
			sb.append(", \"signatures\" : [");
			for(String ui : signatures){
				short[] sigRank = ranks.get(ui);
				sb.append("{\"uid\" : \"").append(ui).append("\", \"ranks\" : ").append(Arrays.toString(sigRank)).append("}, ");
			}
			sb.append("] }");
			
			String json = sb.toString();
			
			
			json = json.replace(", }", "}");
			json = json.replace(", ]", "]");
			out.write(json);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public String md5hash(String plaintext) {
		String hashtext = "new";
		try {
			MessageDigest m = MessageDigest.getInstance("MD5");
			m.reset();
			m.update(plaintext.getBytes());
			byte[] digest = m.digest();
			BigInteger bigInt = new BigInteger(1,digest);
			hashtext = bigInt.toString(16);
			// Now we need to zero pad it if you actually want the full 32 chars.
			while(hashtext.length() < 32 ){
			  hashtext = "0"+hashtext;
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return hashtext;
	}
	
	public static String[] sortByValue(HashMap<String, Double> hm) { 
        // Create a list from elements of HashMap 
        List<Map.Entry<String, Double> > list = new LinkedList<Map.Entry<String, Double> >(hm.entrySet()); 
        
        // Sort the list 
        Collections.sort(list, new Comparator<Map.Entry<String, Double> >() { 
            public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) { 
                return (o1.getValue()).compareTo(o2.getValue()); 
            } 
        });
        
        String[] listKeys = new String[list.size()];
        
        // put data from sorted list to hashmap
        int counter = 0;
        for (Map.Entry<String, Double> me : list) { 
            listKeys[counter] = me.getKey();
            counter++;
        } 
        return listKeys;
    }
	
	<K, V extends Comparable<V>> Map<K, V> sortByValues
    (final Map<K, V> map, int ascending)
	{
	    Comparator<K> valueComparator =  new Comparator<K>() {         
	       private int ascending;
	       public int compare(K k1, K k2) {
	           int compare = map.get(k2).compareTo(map.get(k1));
	           if (compare == 0) return 1;
	           else return ascending*compare;
	       }
	       public Comparator<K> setParam(int ascending)
	       {
	           this.ascending = ascending;
	           return this;
	       }
	   }.setParam(ascending);
	
	   Map<K, V> sortedByValues = new TreeMap<K, V>(valueComparator);
	   sortedByValues.putAll(map);
	   return sortedByValues;
	}
	
}


