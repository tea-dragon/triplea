package games.strategy.triplea.attatchments;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.IAttachment;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.RelationshipType;
import games.strategy.engine.data.ProductionFrontier;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.TechnologyFrontier;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.delegate.BattleTracker;
import games.strategy.triplea.delegate.DelegateFinder;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TechAdvance;
import games.strategy.triplea.delegate.TechTracker;
import games.strategy.triplea.delegate.TripleADelegateBridge;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;


public class TriggerAttachment extends DefaultAttachment{

	/**
	 * 
	 */
	private static final long serialVersionUID = -3327739180569606093L;

	public static final String NOTIFICATION_AFTER = "after";
	public static final String NOTIFICATION_BEFORE = "before";

	private List<RulesAttachment> m_trigger = null;
	private ProductionFrontier m_frontier = null;
	private List<String> m_productionRule = null;
	private boolean m_invert = false;
	private List<TechAdvance> m_tech = new ArrayList<TechAdvance>();
	private Map<Territory,IntegerMap<UnitType>> m_placement = null;
	private IntegerMap<UnitType> m_purchase = null;
	private String m_resource = null;
	private int m_resourceCount = 0;
	private int m_uses = -1;
	private List<PlayerID> m_players= new ArrayList<PlayerID>();
	private Map<UnitSupportAttachment, Boolean> m_support = null;
	private List<String> m_unitProperty = null;
	// List of relationshipChanges that should be executed when this trigger hits.
	private List<String> m_relationshipChange = new ArrayList<String>();

	private UnitType m_unitType = null;
	private Map<String,Map<TechAdvance,Boolean>> m_availableTechs = null;
	private String m_victory = null;
	private String m_conditionType = "AND";
	private String m_notification = null;


	public TriggerAttachment() {
	}
	
	public static Set<TriggerAttachment> getTriggers(PlayerID player, GameData data, Match<TriggerAttachment> cond){
		Set<TriggerAttachment> trigs = new HashSet<TriggerAttachment>();
        Map<String, IAttachment> map = player.getAttachments();
        Iterator<String> iter = map.keySet().iterator();
        while(iter.hasNext() )
        {
        	IAttachment a = map.get(iter.next());
        	if(a instanceof TriggerAttachment && cond.match((TriggerAttachment)a))
        		trigs.add((TriggerAttachment)a);
        }
        return trigs;
	}
	public void setTrigger(String triggers) throws GameParseException{
		String[] s = triggers.split(":");
		for(int i = 0;i<s.length;i++) {
			RulesAttachment trigger = null;
			for(PlayerID p:getData().getPlayerList().getPlayers()){
				trigger = (RulesAttachment) p.getAttachment(s[i]);
				if( trigger != null)
					break;
			}
			if(trigger == null)
				throw new GameParseException("Triggers: Could not find rule. name:" + s[i]);
			if(m_trigger == null)
				m_trigger = new ArrayList<RulesAttachment>();
			m_trigger.add(trigger);
		}
	}
	
	public List<RulesAttachment> getTrigger() {
		return m_trigger;
	}
	
	public void setFrontier(String s) throws GameParseException{
		ProductionFrontier front = getData().getProductionFrontierList().getProductionFrontier(s);
		if(front == null)
            throw new GameParseException("Triggers: Could not find frontier. name:" + s);
        m_frontier = front;
	}
	
	public ProductionFrontier getFrontier() {
		return m_frontier;
	}
	
	/**
	 * add, not set.
	 */
	public void setProductionRule(String prop) throws GameParseException{
		String[] s = prop.split(":");
		if(s.length!=2) 
    		throw new GameParseException("Triggers: Invalid productionRule declaration: " + prop);
		if(m_productionRule== null)
			m_productionRule = new ArrayList<String>();
		if(getData().getProductionFrontierList().getProductionFrontier(s[0]) == null)
			throw new GameParseException("Triggers: Could not find frontier. name:" + s[0]);
		String rule = s[1];
		if (rule.startsWith("-"))
			rule = rule.replaceFirst("-", "");
		if (getData().getProductionRuleList().getProductionRule(rule) == null)
			throw new GameParseException("Triggers: Could not find production rule. name:" + rule);
		m_productionRule.add(prop);
	}
	
	public List<String> getProductionRule() {
		return m_productionRule;
	}
	
	public int getResourceCount() {
		return m_resourceCount;
	}
	public void setResourceCount(String s) {
		m_resourceCount = getInt(s);
	}

	public void setUses(String s) {
		m_uses = getInt(s);
	}
	public void setUses(Integer u) {
		m_uses = u;
	}
	
	public int getUses() {
		return m_uses;
	}
	
	public boolean getInvert() {
		return m_invert;
	}
	
	public void setInvert(String s) {
		m_invert = getBool(s);
	}
	
	public String getVictory() {
		return m_victory;
	}
	
	public void setVictory(String s) {
		m_victory = s;
	}
	
	public void setNotification(String sNotification) throws GameParseException {
		String[] s = sNotification.split(":");
		if(s.length != 3)
			throw new GameParseException("Triggers: notification must exist in 3 parts: \"before/after:stepName:MessageId\".");
		if(!(s[0].equals(NOTIFICATION_AFTER) || s[0].equals(NOTIFICATION_BEFORE)))
			throw new GameParseException("Triggers: notificaition must start with: "+NOTIFICATION_BEFORE+" or "+NOTIFICATION_AFTER);
		m_notification = sNotification;
	}
	
	protected String getNotification() {
		return m_notification;
	}
	
	protected String getNotificationStepName() {
		if(m_notification == null)
			return null;
		String[] s = m_notification.split(":");
		return s[1];
	}
	
	protected String getNotificationBeforeOrAfter() {
		if(m_notification == null)
			return null;
		String[] s = m_notification.split(":");
		return s[0];	}
	
	public String getNotificationMessage() {
		if(m_notification == null)
			return null;
		String[] s = m_notification.split(":");
		return s[2];
	}
	
	public String getConditionType() {
		return m_conditionType;
	}
	
	public void setConditionType(String s) throws GameParseException{
		if (!(s.equals("and") || s.equals("AND") || s.equals("or") || s.equals("OR") || s.equals("XOR") || s.equals("xor")))
			throw new GameParseException("Triggers: conditionType must be equal to AND or OR or XOR");
		m_conditionType = s;
	}
	
	public List<TechAdvance> getTech() {
		return m_tech;
	}
	
	public void setTech(String techs) throws GameParseException{
		String[] s = techs.split(":");
		for(int i = 0;i<s.length;i++){
			TechAdvance ta = getData().getTechnologyFrontier().getAdvanceByProperty(s[i]);
			if(ta==null)
				ta = getData().getTechnologyFrontier().getAdvanceByName(s[i]);
			if(ta==null)
				throw new GameParseException("Triggers: Technology not found :"+s[i]);
			m_tech.add(ta);
		}
	}
	
	public Map<String,Map<TechAdvance,Boolean>> getAvailableTech() {
		return m_availableTechs;
	}
	
	public void setAvailableTech(String techs) throws GameParseException{
		String[] s = techs.split(":");
		if(s.length<2)
    		throw new GameParseException( "Triggers: Invalid tech availability: "+techs+ " should be category:techs");
		String cat = s[0]; 
		Map<TechAdvance,Boolean> tlist = new LinkedHashMap<TechAdvance,Boolean>(); 
		for(int i = 1;i<s.length;i++){
			boolean add = true;
			if( s[i].startsWith("-")) {
				add = false;
				s[i] = s[i].substring(1);
			}
			TechAdvance ta = getData().getTechnologyFrontier().getAdvanceByProperty(s[i]);
			if(ta==null)
				ta = getData().getTechnologyFrontier().getAdvanceByName(s[i]);
			if(ta==null)
				throw new GameParseException("Triggers: Technology not found :"+s[i]);
			tlist.put(ta,add);
		}
		if(m_availableTechs == null)
			m_availableTechs = new HashMap<String,Map<TechAdvance,Boolean>>();
		if(m_availableTechs.containsKey(cat))
			tlist.putAll(m_availableTechs.get(cat));
		m_availableTechs.put(cat, tlist);
	}
	
	public UnitType getUnitType() {
		return m_unitType;
	}
	public void setUnitType(String name) throws GameParseException
    {
            UnitType type = getData().getUnitTypeList().getUnitType(name);
            if(type == null)
                throw new GameParseException("Triggers: Could not find unitType. name:" + name);
            m_unitType = type;
    	
    }
	public Map<UnitSupportAttachment, Boolean> getSupport() {
		return m_support;
	}
	
	public void setSupport(String sup) throws GameParseException{
		String[] s = sup.split(":");
		for(int i =0;i<s.length;i++) {
			boolean add = true;
			if( s[i].startsWith("-")) {
				add = false;
				s[i] = s[i].substring(1);
			}
			boolean found = false;
			for(UnitSupportAttachment support:UnitSupportAttachment.get(getData())) {
				if( support.getName().equals(s[i])) {
					found = true;
					if(m_support == null)
						m_support = new LinkedHashMap<UnitSupportAttachment,Boolean>();
					m_support.put(support, add);
					break;
				}
			}
			if(!found)
				throw new GameParseException("Triggers: Could not find unitSupportAttachment. name:" + s[i]);
		}
		
	}
	public void setPlayers(String names) throws GameParseException
	{
		String[] s = names.split(":");
		for(int i =0;i<s.length;i++) {
			PlayerID player = getData().getPlayerList().getPlayerID(s[i]);
			if(player == null)
				throw new GameParseException("Triggers: Could not find player. name:" + s[i]);
			m_players.add(player);
		}
	}
	public List<PlayerID> getPlayers() {
		if(m_players.isEmpty()) 
			return Collections.singletonList((PlayerID)getAttatchedTo());
		else
			return m_players;
    }
	public String getResource() {
		return m_resource;
	}
	
	public void setResource(String s) throws GameParseException{
		Resource r = getData().getResourceList().getResource(s);
		if( r == null )
			throw new GameParseException( "Triggers: Invalid resource: " +s);
		else
			m_resource = s;
	}
	
	public List<String> getRelationshipChange() {
		return m_relationshipChange;
	}
	
	public void setRelationshipChange(String relChange) throws GameParseException {
		String[] s = relChange.split(":");
		if(s.length !=3)
			throw new GameParseException("Triggers: Invalid relationshipChange declaration: "+relChange+" \n Use: player:oldRelation:newRelation\n");
		if(getData().getPlayerList().getPlayerID(s[0]) == null)
			throw new GameParseException("Triggers: Invalid relationshipChange declaration: "+relChange+" \n player: "+s[0]+" unknown in: "+getName());
		
		if(!(s[1].equals(Constants.RELATIONSHIP_CONDITION_ANY_NEUTRAL) || 
				s[1].equals(Constants.RELATIONSHIP_CONDITION_ANY) ||
				s[1].equals(Constants.RELATIONSHIP_CONDITION_ANY_ALLIED) ||
				s[1].equals(Constants.RELATIONSHIP_CONDITION_ANY_WAR) ||
				Matches.isValidRelationshipName(getData()).match(s[1])))
			throw new GameParseException("Triggers: Invalid relationshipChange declaration: "+relChange+" \n relationshipType: "+s[1]+" unknown in: "+getName());
		
		if(Matches.isValidRelationshipName(getData()).invert().match(s[2]))
			throw new GameParseException("Triggers: Invalid relationshipChange declaration: "+relChange+" \n relationshipType: "+s[2]+" unknown in: "+getName());
		
		m_relationshipChange.add(relChange);
	}
	
	public List<String> getUnitProperty() {
		return m_unitProperty;
	}
	
	// add not set
	public void setUnitProperty(String prop) throws GameParseException{
		String[] s = prop.split(":");
		if(s.length!=2) 
    		throw new GameParseException( "Triggers: Invalid unitProperty declaration: " +prop);
		if(m_unitProperty== null)
			m_unitProperty = new ArrayList<String>();
		prop = s[1]+":"+s[0];
		m_unitProperty.add(prop);
		
	}
	public Map<Territory,IntegerMap<UnitType>> getPlacement() {
		return m_placement;
	}
	// fudging this, it really represents adding placements
	public void setPlacement(String place) throws GameParseException{
		String[] s = place.split(":");
    	int count = -1,i=0;
    	if(s.length<1)
    		throw new GameParseException( "Triggers: Empty placement list");
    	try {
    		count = getInt(s[0]);
    		i++;
    	} catch(Exception e) {
    		count = 1;
    	}
    	if(s.length<1 || s.length ==1 && count != -1)
    		throw new GameParseException( "Triggers: Empty placement list");
    	Territory territory = getData().getMap().getTerritory(s[i]);
    	if( territory == null )
			throw new GameParseException( "Triggers: Territory does not exist " + s[i]);
    	else {
    		i++;
    		IntegerMap<UnitType> map = new IntegerMap<UnitType>();
    		for( ; i < s.length; i++){
    			UnitType type = getData().getUnitTypeList().getUnitType(s[i]);
    			if(type == null)
    				throw new GameParseException( "Triggers: UnitType does not exist " + s[i]);
    			else
    				map.add(type, count);
    		}
    		if( m_placement == null)
    			m_placement = new HashMap<Territory,IntegerMap<UnitType>>();
    		if(m_placement.containsKey(territory))
    			map.add(m_placement.get(territory));
    		m_placement.put(territory, map);
    	}	
	}
	
	public void setPurchase(String place) throws GameParseException{
		String[] s = place.split(":");
    	int count = -1,i=0;
    	if(s.length<1)
    		throw new GameParseException( "Triggers: Empty purchase list");
    	try {
    		count = getInt(s[0]);
    		i++;
    	} catch(Exception e) {
    		count = 1;
    	}
    	if(s.length<1 || s.length ==1 && count != -1)
    		throw new GameParseException( "Triggers: Empty purchase list");
    	else {
    		if(m_purchase == null ) 
    			m_purchase = new IntegerMap<UnitType>();
    		for( ; i < s.length; i++){
    			UnitType type = getData().getUnitTypeList().getUnitType(s[i]);
    			if(type == null)
    				throw new GameParseException( "Triggers: UnitType does not exist " + s[i]);
    			else
    				m_purchase.add(type, count);
    		}	
    	}


	}
	public IntegerMap<UnitType> getPurchase() {
		return m_purchase;
	}
	
	/**
	 * This will account for Invert and conditionType
	 */
	private static boolean isMet(TriggerAttachment t, GameData data) {
		boolean met = false;
		String conditionType = t.getConditionType();
		if (conditionType.equals("AND") || conditionType.equals("and"))
		{
			for (RulesAttachment c:t.getTrigger()) {
				met = c.isSatisfied(data) != t.getInvert();
				if (!met)
					break;
			}
		}
		else if (conditionType.equals("OR") || conditionType.equals("or"))
		{
			for (RulesAttachment c:t.getTrigger()) {
				met = c.isSatisfied(data) != t.getInvert();
				if (met)
					break;
			}
		}
		else if (conditionType.equals("XOR") || conditionType.equals("xor"))
		{
			// XOR is confusing with more than 2 conditions, so we will just say that one has to be true, while all others must be false
			boolean isOneTrue = false;
			for (RulesAttachment c:t.getTrigger()) {
				met = c.isSatisfied(data) != t.getInvert();
				if (isOneTrue && met)
				{
					isOneTrue = false;
					break;
				}
				else if (met)
					isOneTrue = true;
			}
			met = isOneTrue;
		}
		
		return met;
	}
	
	
	
	
	public static void triggerProductionChange(PlayerID player, IDelegateBridge aBridge, GameData data) {
		Set<TriggerAttachment> trigs = getTriggers(player,data,prodMatch);
		CompositeChange change = new CompositeChange();
		for(TriggerAttachment t:trigs) {
			boolean met = isMet(t, data);
			if(met)
			{
				t.use(aBridge);
				for( PlayerID aPlayer: t.getPlayers()){
					change.add(ChangeFactory.changeProductionFrontier(aPlayer, t.getFrontier()));
					aBridge.getHistoryWriter().startEvent("Triggers: " + aPlayer.getName() + " has their production frontier changed to: " + t.getFrontier().toString());
				}
			}
		}
		if( !change.isEmpty())
			aBridge.addChange(change);
	}
	
	public static void triggerProductionFrontierEditChange(PlayerID player, IDelegateBridge aBridge, GameData data) {
		Set<TriggerAttachment> trigs = getTriggers(player, data, prodFrontierEditMatch);
		CompositeChange change = new CompositeChange();
		for(TriggerAttachment t:trigs) {
			boolean met = isMet(t, data);
			if(met)
			{
				t.use(aBridge);
				Iterator<String> iter = t.getProductionRule().iterator();
				while (iter.hasNext())
				{
					boolean add = true;
					String[] s = iter.next().split(":");
					ProductionFrontier front = data.getProductionFrontierList().getProductionFrontier(s[0]);
					String rule = s[1];
					if (rule.startsWith("-"))
					{
						rule = rule.replaceFirst("-", "");
						add = false;
					}
					ProductionRule pRule = data.getProductionRuleList().getProductionRule(rule);
					
					if (add) 
					{
						if (!front.getRules().contains(pRule))
						{
							change.add(ChangeFactory.addProductionRule(pRule, front));
							aBridge.getHistoryWriter().startEvent("Triggers: " + pRule.getName() + " added to " + front.getName());
						}
					}
					else
					{
						if (front.getRules().contains(pRule))
						{
							change.add(ChangeFactory.removeProductionRule(pRule, front));
							aBridge.getHistoryWriter().startEvent("Triggers: " + pRule.getName() + " removed from " + front.getName());
						}
					}
				}
			}
		}
		if( !change.isEmpty())
			aBridge.addChange(change); // TODO: we should sort the frontier list if we make changes to it...
	}
	
	public static String triggerVictory(PlayerID player, IDelegateBridge aBridge, GameData data) {
		Set<TriggerAttachment> trigs = getTriggers(player,data,victoryMatch);
		for(TriggerAttachment t:trigs) {
			boolean met = isMet(t, data);
			if(met) {
				t.use(aBridge);
				// no need for history writing as the method calling this has its own history writer
				for( PlayerID aPlayer: t.getPlayers()){
					return t.getVictory();
				}
			}
		}
		return null;
	}
	
	public static void triggerTechChange(PlayerID player, IDelegateBridge aBridge, GameData data) {
		Set<TriggerAttachment> trigs = getTriggers(player,data,techMatch);
		for(TriggerAttachment t:trigs) {
			boolean met = isMet(t, data);
			if(met) {
				t.use(aBridge);
				for( PlayerID aPlayer: t.getPlayers()){
					for( TechAdvance ta:t.getTech()) {
						if(ta.hasTech(TechAttachment.get(aPlayer))
								|| !TechAdvance.getTechAdvances(data, aPlayer).contains(ta))
							continue;
						aBridge.getHistoryWriter().startEvent("Triggers: " + aPlayer.getName() + " activates " + ta);
						TechTracker.addAdvance(aPlayer, data, aBridge, ta);
					}
				}
			}
		}
	}
	
	public static void triggerAvailableTechChange(PlayerID player, IDelegateBridge aBridge, GameData data) {
		Set<TriggerAttachment> trigs = getTriggers(player,data,techAMatch);
		for(TriggerAttachment t:trigs) {
			boolean met = isMet(t, data);
			if(met) {
				t.use(aBridge);
				for( PlayerID aPlayer: t.getPlayers()){
					for(String cat:t.getAvailableTech().keySet()){
						TechnologyFrontier tf = aPlayer.getTechnologyFrontierList().getTechnologyFrontier(cat);
						if(tf == null)
							throw new IllegalStateException("Triggers: tech category doesn't exist:"+cat+" for player:"+aPlayer);
						for(TechAdvance ta: t.getAvailableTech().get(cat).keySet()){
							if(t.getAvailableTech().get(cat).get(ta)) {
								aBridge.getHistoryWriter().startEvent("Triggers: " + aPlayer.getName() + " gains access to " + ta);
								Change change = ChangeFactory.addAvailableTech(tf, ta,aPlayer);
								aBridge.addChange(change);
							}
							else {
								aBridge.getHistoryWriter().startEvent("Triggers: " + aPlayer.getName() + " loses access to " + ta);
								Change change = ChangeFactory.removeAvailableTech(tf, ta,aPlayer);
								aBridge.addChange(change);
							}
						}
					}
				}
			}
		}
	}
	
	public static void triggerUnitPlacement(PlayerID player, IDelegateBridge aBridge, GameData data) {
		Set<TriggerAttachment> trigs = getTriggers(player,data,placeMatch);
		for(TriggerAttachment t:trigs) {
			boolean met = isMet(t, data);
			if(met) {
				t.use(aBridge);
				for( PlayerID aPlayer: t.getPlayers()){
					for(Territory ter: t.getPlacement().keySet()) {
						//aBridge.getHistoryWriter().startEvent("Triggers: " + aPlayer.getName() + " places " + t.getPlacement().get(ter).toString() + " in territory " + ter.getName());
						placeUnits(ter,t.getPlacement().get(ter),aPlayer,data,aBridge);
					}
				}
			}
		}
	}
	
	public static void triggerPurchase(PlayerID player, IDelegateBridge aBridge, GameData data) {
		Set<TriggerAttachment> trigs = getTriggers(player,data,purchaseMatch);
		for(TriggerAttachment t:trigs) {
			boolean met = isMet(t, data);
			if(met) {
				t.use(aBridge);
				for( PlayerID aPlayer: t.getPlayers()){
					List<Unit> units = new ArrayList<Unit>();
					for(UnitType u: t.getPurchase().keySet()) {
						units.addAll(u.create(t.getPurchase().getInt(u), aPlayer));
					}
					if(!units.isEmpty()) {
						String transcriptText = "Triggers: " + MyFormatter.unitsToTextNoOwner(units) + " gained by " + aPlayer;
						aBridge.getHistoryWriter().startEvent(transcriptText);
						aBridge.getHistoryWriter().setRenderingData(units);
						Change place = ChangeFactory.addUnits(aPlayer, units);
						aBridge.addChange(place);
					}
				}
			}	
		}
	}
	
	public static void triggerResourceChange(PlayerID player, IDelegateBridge aBridge, GameData data) {
		Set<TriggerAttachment> trigs = getTriggers(player,data,resourceMatch);
		CompositeChange change = new CompositeChange();
		for(TriggerAttachment t:trigs) {
			boolean met = isMet(t, data);
			if(met) {
				t.use(aBridge);
				for( PlayerID aPlayer: t.getPlayers()){
					int toAdd = t.getResourceCount();
					if(t.getResource().equals(Constants.PUS));
						toAdd *= Properties.getPU_Multiplier(data);
					int total = aPlayer.getResources().getQuantity(t.getResource()) + toAdd;
					if(total < 0) {
						toAdd -= total;
						total = 0;
					}
					change.add(ChangeFactory.changeResourcesChange(aPlayer, data.getResourceList().getResource(t.getResource()), toAdd));
					String PUMessage = "Triggers: " + aPlayer.getName() + " met a national objective for an additional " + t.getResourceCount() + " " + t.getResource()+
					"; end with " + total + " " +t.getResource();
					aBridge.getHistoryWriter().startEvent(PUMessage);
				}
			}
				
		}
		if( !change.isEmpty())
			aBridge.addChange(change);
	}
	
	// note this change is silent
	public static void triggerSupportChange(PlayerID player, IDelegateBridge aBridge, GameData data) {
		Set<TriggerAttachment> trigs = getTriggers(player,data,supportMatch);
		CompositeChange change = new CompositeChange();
		for(TriggerAttachment t:trigs) {
			boolean met = isMet(t, data);
			if(met) {
				t.use(aBridge);
				for( PlayerID aPlayer: t.getPlayers()){
					for(UnitSupportAttachment usa:t.getSupport().keySet()){
					List<PlayerID> p = new ArrayList<PlayerID>(usa.getPlayers());
					if(p.contains(aPlayer)) {
						if(!t.getSupport().get(usa)) {
							p.remove(aPlayer);
							change.add(ChangeFactory.attachmentPropertyChange(usa, p, "players"));
							aBridge.getHistoryWriter().startEvent("Triggers: " + aPlayer.getName() + " is removed from " + usa.toString());
						}
					}
					else {
						if(t.getSupport().get(usa)) {
							p.add(aPlayer);
							change.add(ChangeFactory.attachmentPropertyChange(usa, p, "players"));
							aBridge.getHistoryWriter().startEvent("Triggers: " + aPlayer.getName() + " is added to " + usa.toString());
						}	
					}
					}
				}
			}
		}
		if( !change.isEmpty())
			aBridge.addChange(change);
	}

	public static void triggerRelationshipChange(PlayerID player,IDelegateBridge aBridge, GameData data) {
		Set<TriggerAttachment> trigs = getTriggers(player,data,relationshipChangeMatch);
		CompositeChange change = new CompositeChange();
		for(TriggerAttachment t: trigs) {
			if(isMet(t,data)) {
				t.use(aBridge);
				for(String relationshipChange:t.getRelationshipChange()) {
					String[] s = relationshipChange.split(":");
					PlayerID player2 = data.getPlayerList().getPlayerID(s[0]);
					RelationshipType currentRelation = data.getRelationshipTracker().getRelationshipType(player, player2);
					
					if(  s[1].equals(Constants.RELATIONSHIP_CONDITION_ANY) || 
							(s[1].equals(Constants.RELATIONSHIP_CONDITION_ANY_NEUTRAL) && Matches.RelationshipIsNeutral.match(currentRelation)) ||
							(s[1].equals(Constants.RELATIONSHIP_CONDITION_ANY_ALLIED)  && Matches.RelationshipIsAllied.match(currentRelation)) ||
							(s[1].equals(Constants.RELATIONSHIP_CONDITION_ANY_WAR)     && Matches.RelationshipIsAtWar.match(currentRelation)) ||
							currentRelation.equals(data.getRelationshipTypeList().getRelationshipType(s[1]))) {
				
						RelationshipType triggerNewRelation = data.getRelationshipTypeList().getRelationshipType(s[2]);
						change.add(ChangeFactory.relationshipChange(player, player2, currentRelation,triggerNewRelation));
						aBridge.getHistoryWriter().startEvent("Triggers: Changing Relationship for "+player.getName()+" and "+player2.getName()+" from "+currentRelation.getName()+" to "+triggerNewRelation.getName());
						if(Matches.RelationshipIsAtWar.match(triggerNewRelation))
							triggerMustFightBattle(player,player2,aBridge,data);
					}
				}
			}
			
		}
		if( !change.isEmpty())
			aBridge.addChange(change);		
	}
	
	private static void triggerMustFightBattle(PlayerID player1, PlayerID player2, IDelegateBridge aBridge, GameData data) {
		for (Territory terr : Match.getMatches(data.getMap().getTerritories(), Matches.territoryHasUnitsOwnedBy(player1))) {
			if (Matches.territoryHasEnemyUnits(player1, data).match(terr))
				DelegateFinder.battleDelegate(data).getBattleTracker().addBattle(new CRoute(terr), terr.getUnits().getMatches(Matches.unitIsOwnedBy(player1)), false, player1, data, aBridge, null);
		}
	}
	
	private static BattleTracker getBattleTracker(GameData data) {
		return DelegateFinder.battleDelegate(data).getBattleTracker();
	}

	public static Set<String> triggerNotifications(String beforeOrAfter, PlayerID player, GameData data) {
		String stepName = data.getSequence().getStep().getName();
		Set<TriggerAttachment> trigs = getTriggers(player,data,getNotificationStepTriggerMatch(beforeOrAfter,stepName));
		Set<String> notifications = new HashSet<String>();
		for(TriggerAttachment t:trigs) {
			if(isMet(t,data)) {
				t.use(data.getSequence().getStep().getDelegate().getBridge());
				notifications.add(t.getNotificationMessage());
			}
		}
		return notifications;	
	}


	public static void triggerUnitPropertyChange(PlayerID player, IDelegateBridge aBridge, GameData data) {
		Set<TriggerAttachment> trigs = getTriggers(player,data,unitPropertyMatch);
		CompositeChange change = new CompositeChange();
		for(TriggerAttachment t:trigs) {
			boolean met = isMet(t, data);
			if(met) {
				t.use(aBridge);
				for(String property:t.getUnitProperty()) {
					String[] s = property.split(":");
					if(UnitAttachment.get(t.getUnitType()).getRawProperty(s[0]).equals(s[1]))
						continue;
					change.add(ChangeFactory.attachmentPropertyChange(UnitAttachment.get(t.getUnitType()), property, "rawProperty"));
					aBridge.getHistoryWriter().startEvent("Triggers: Setting " + s[0]+ " to " + s[1] + " for " + t.getUnitType().getName());
				}
			}	
		}
		if( !change.isEmpty())
			aBridge.addChange(change);
	}
	
	private static void placeUnits(Territory terr, IntegerMap<UnitType> uMap,PlayerID player,GameData data,IDelegateBridge aBridge){
		// createUnits
		List<Unit> units = new ArrayList<Unit>();;
		for(UnitType u: uMap.keySet()) {
			units.addAll(u.create(uMap.getInt(u), player));
		}
		CompositeChange change = new CompositeChange();
		// mark no movement
		for(Unit unit:units){
			UnitAttachment ua = UnitAttachment.get(unit.getType());
	        change.add(ChangeFactory.unitPropertyChange(unit, ua.getMovement(unit.getOwner()), TripleAUnit.ALREADY_MOVED));
		}
		// place units
        Collection<Unit> factoryAndAA = Match.getMatches(units,
                Matches.UnitIsAAOrIsFactoryOrIsInfrastructure);
        change.add(DelegateFinder.battleDelegate(data).getOriginalOwnerTracker()
                .addOriginalOwnerChange(factoryAndAA, player));
       
        String transcriptText = "Triggers: " + player.getName() + " has " + MyFormatter.unitsToTextNoOwner(units) + " placed in " + terr.getName();
        aBridge.getHistoryWriter().startEvent(transcriptText);
        aBridge.getHistoryWriter().setRenderingData(units);

        Change place = ChangeFactory.addUnits(terr, units);
        change.add(place);
        
        /* No longer needed, as territory unitProduction is now set by default to equal the territory value. Therefore any time it is different from the default, the map maker set it, so we shouldn't screw with it.
        if(Match.someMatch(units, Matches.UnitIsFactoryOrCanProduceUnits) && !Match.someMatch(terr.getUnits().getUnits(), Matches.UnitIsFactoryOrCanProduceUnits) && games.strategy.triplea.Properties.getSBRAffectsUnitProduction(data))
        {
        	// if no factories are there, make sure the territory has no damage (that unitProduction = production)
        	TerritoryAttachment ta = TerritoryAttachment.get(terr);
        	int prod = 0;
        	if(ta != null)
        		prod = ta.getProduction();
        	
            Change unitProd = ChangeFactory.changeUnitProduction(terr, prod);
            change.add(unitProd);
        }*/

        aBridge.addChange(change);
        // handle adding to enemy territories
        if( Matches.isTerritoryEnemyAndNotUnownedWaterOrImpassibleOrRestricted(player, data).match(terr))
        	getBattleTracker(data).addBattle(new CRoute(terr), units, false, player, data, aBridge, null);
	}
	
	
	
	private void use (IDelegateBridge aBridge) {
		if( m_uses > 0) {
			aBridge.addChange(ChangeFactory.attachmentPropertyChange(this, new Integer(m_uses-1).toString(), "uses"));
		}
	}
	
	private static Match<TriggerAttachment> prodMatch = new Match<TriggerAttachment>()
	{
		public boolean match(TriggerAttachment t)
		{
			return t.getFrontier() != null && t.getUses()!=0;
		}
	};
	
	private static Match<TriggerAttachment> prodFrontierEditMatch = new Match<TriggerAttachment>()
	{
		public boolean match(TriggerAttachment t)
		{
			return t.getProductionRule() != null && t.getProductionRule().size() > 0 && t.getUses()!=0;
		}
	};

	private static Match<TriggerAttachment> techMatch = new Match<TriggerAttachment>()
	{
		public boolean match(TriggerAttachment t)
		{
			return !t.getTech().isEmpty() && t.getUses()!=0;
		}
	};
	
	private static Match<TriggerAttachment> techAMatch = new Match<TriggerAttachment>()
	{
		public boolean match(TriggerAttachment t)
		{
			return t.getAvailableTech() != null && t.getUses()!=0;
		}
	};
	private static Match<TriggerAttachment> placeMatch = new Match<TriggerAttachment>()
	{
		public boolean match(TriggerAttachment t)
		{
			return t.getPlacement() != null && t.getUses()!=0;
		}
	};
	
	private static Match<TriggerAttachment> purchaseMatch = new Match<TriggerAttachment>()
	{
		public boolean match(TriggerAttachment t)
		{
			return t.getPurchase() != null && t.getUses()!=0;
		}
	};
	
	private static Match<TriggerAttachment> resourceMatch = new Match<TriggerAttachment>()
	{
		public boolean match(TriggerAttachment t)
		{
			return t.getResource() != null && t.getResourceCount() != 0 && t.getUses()!=0;
		}
	};
	
	private static Match<TriggerAttachment> supportMatch = new Match<TriggerAttachment>()
	{
		public boolean match(TriggerAttachment t)
		{
			return t.getSupport() != null && t.getUses()!=0;
		}
	};
	
	private static Match<TriggerAttachment> unitPropertyMatch = new Match<TriggerAttachment>()
	{
		public boolean match(TriggerAttachment t)
		{
			return t.getUnitType() != null && t.getUnitProperty() !=null && t.getUses()!=0;
		}
	};
	
	private static Match<TriggerAttachment> relationshipChangeMatch = new Match<TriggerAttachment>()
	{
		public boolean match(TriggerAttachment t)
		{
			return !(t.getRelationshipChange().isEmpty() || t.getUses()==0);
		}
	};
	
	private static Match<TriggerAttachment> getNotificationStepTriggerMatch(final String beforeOrAfter, final String stepName) {
		return new Match<TriggerAttachment>() {
			public boolean match(TriggerAttachment t) {
				return t.getNotification() != null && stepName.equals(t.getNotificationStepName()) && beforeOrAfter.equals(t.getNotificationBeforeOrAfter()) && t.getNotificationMessage().length() > 0 && t.getUses()!= 0;
			}
		};
	}


	private static Match<TriggerAttachment> victoryMatch = new Match<TriggerAttachment>()
	{
		public boolean match(TriggerAttachment t)
		{
			return t.getVictory() != null && t.getVictory().length() > 0 && t.getUses()!=0;
		}
	};
	
	public void validate(GameData data) throws GameParseException
	{
		if( m_trigger==null)
		throw new GameParseException("Triggers: Invalid Unit attatchment" + this);
	}
	  
	// shameless cheating. making a fake route, so as to handle battles 
	// properly without breaking battleTracker protected status or duplicating 
	// a zillion lines of code.
	private static class CRoute extends Route {
		private static final long serialVersionUID = -4571007882522107666L;
		public CRoute(Territory terr) {
			super(terr);
		}
		public Territory getEnd()
	    {
	        return getStart();
	    }
		public int getLength()
	    {
	        return 1;
	    }
		public Territory at(int i)
	    {
	        return getStart();
	    }
	}


}
