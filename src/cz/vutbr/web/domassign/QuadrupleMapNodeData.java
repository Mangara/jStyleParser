package cz.vutbr.web.domassign;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cz.vutbr.web.css.CSSFactory;
import cz.vutbr.web.css.CSSProperty;
import cz.vutbr.web.css.Declaration;
import cz.vutbr.web.css.NodeData;
import cz.vutbr.web.css.SupportedCSS;
import cz.vutbr.web.css.Term;
import cz.vutbr.web.csskit.OutputUtil;

public class QuadrupleMapNodeData implements NodeData {

	private static final int COMMON_DECLARATION_SIZE = 7;
	
	protected static DeclarationTransformer transformer = DeclarationTransformer.getInstance();
	protected static SupportedCSS css = CSSFactory.getSupportedCSS();

	private Map<String,CSSProperty> propertiesOwn;
	private Map<String,CSSProperty> propertiesInh;
	private Map<String,Term<?>> valuesOwn;
	private Map<String,Term<?>> valuesInh;
	
	public QuadrupleMapNodeData() {
		this.propertiesOwn = new HashMap<String, CSSProperty>(css.getTotalProperties(), 1.0f);
		this.propertiesInh = new HashMap<String, CSSProperty>(css.getTotalProperties(), 1.0f);
		this.valuesOwn = new HashMap<String, Term<?>>(css.getTotalProperties(), 1.0f);
		this.valuesInh = new HashMap<String, Term<?>>(css.getTotalProperties(), 1.0f);
	}
	
	public <T extends CSSProperty> T getProperty(Class<T> clazz, String name, boolean includeInherited) {

		T inherited = null;
		
		if(includeInherited) 
			inherited = clazz.cast(propertiesInh.get(name));

		T own = clazz.cast(propertiesOwn.get(name));
		if(own==null) return inherited;
		return own;
	}
	
	public <T extends CSSProperty> T getProperty(Class<T> clazz, String name) {
		return getProperty(clazz, name, true);
	}
	
	public <T extends Term<?>> T getValue(Class<T> clazz, String name, boolean includeInherited) {
		
		T inherited = null;
		
		if(includeInherited) 
			inherited = clazz.cast(valuesInh.get(name));

		T own = clazz.cast(valuesOwn.get(name));
		if(own==null) return inherited;
		return own;
	}
	
	public <T extends Term<?>> T getValue(Class<T> clazz, String name) {
		return getValue(clazz, name, true);
	}
	
	public void push(Declaration d) {
		
		Map<String,CSSProperty> properties = 
			new HashMap<String,CSSProperty>(COMMON_DECLARATION_SIZE);
		Map<String,Term<?>> terms = 
			new HashMap<String, Term<?>>(COMMON_DECLARATION_SIZE);
		
		boolean result = transformer.parseDeclaration(d, properties, terms);
		
		// in case of false do not insert anything
		if(!result) return;
		
		this.propertiesOwn.putAll(properties);
		this.valuesOwn.putAll(terms);
		
	}
	
	public NodeData inheritFrom(NodeData parent) {
		
		if(parent==null)
			return this;
		
		if(!(parent instanceof QuadrupleMapNodeData))
			throw new IllegalArgumentException(
					"Cant't inherit from NodeData different from "
							+ this.getClass().getName() + "("+ parent.getClass().getName()+")");
		
		QuadrupleMapNodeData nd = (QuadrupleMapNodeData) parent;
		
		// inherit values
		for(String key:nd.propertiesInh.keySet()) {
			CSSProperty value = nd.propertiesInh.get(key);
			if(value.inherited()) {
				this.propertiesInh.put(key, value);
				Term<?> term = nd.valuesInh.get(key);
				if(term!=null)
					this.valuesInh.put(key, term);
			}
		}
		
		for(String key:nd.propertiesOwn.keySet()) {
			CSSProperty value = nd.propertiesOwn.get(key);
			if(value.inherited()) {
				this.propertiesInh.put(key, value);
				Term<?> term = nd.valuesOwn.get(key);
				if(term!=null)
					this.valuesInh.put(key, term);
			}
		}		
	
		return this;
	}
	
	public NodeData concretize() {
		
		// inherited firstly, replace them with defaults
		for(String key: propertiesInh.keySet()) {
			CSSProperty p = propertiesInh.get(key);
			if(p.equalsInherit()) {
				propertiesInh.put(key, css.getDefaultProperty(key));
				Term<?> value = css.getDefaultValue(key);
				if(value!=null) valuesInh.put(key, value);
			}
		
		}
		
		// own after, replace them with inherited or default
		for(String key:propertiesOwn.keySet()) {
			CSSProperty p = propertiesOwn.get(key);
			if(p.equalsInherit()) {
				CSSProperty rp = propertiesInh.get(key);
				if(rp==null) rp = css.getDefaultProperty(key);
				
				propertiesOwn.put(key, rp);
				
				Term<?> value = valuesInh.get(key);
				if(value==null) value = css.getDefaultValue(key);
				if(value!=null) valuesOwn.put(key, value);
			}
		}
		
		return this;
	}

	@Override
	public String toString() {
		
		StringBuilder sb = new StringBuilder();
		
		
		Set<String> tmp = new LinkedHashSet<String>();
		tmp.addAll(propertiesInh.keySet());
		tmp.addAll(propertiesOwn.keySet());
		
		List<String> keys = new ArrayList<String>(tmp);
		Collections.sort(keys);

		for(String key:keys) {
			// always use own value if exists
			CSSProperty prop = propertiesOwn.get(key);
			if(prop==null) prop = propertiesInh.get(key);

			Term<?> value = valuesOwn.get(key);
			if(value==null) value = valuesInh.get(key);
			
			sb.append(key).append(OutputUtil.PROPERTY_OPENING);
			
			if(value!=null) sb.append(value.toString());
			else sb.append(prop.toString());
				
			sb.append(OutputUtil.PROPERTY_CLOSING);
			
		}
		
		return sb.toString();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((propertiesInh == null) ? 0 : propertiesInh.hashCode());
		result = prime * result
				+ ((propertiesOwn == null) ? 0 : propertiesOwn.hashCode());
		result = prime * result
				+ ((valuesInh == null) ? 0 : valuesInh.hashCode());
		result = prime * result
				+ ((valuesOwn == null) ? 0 : valuesOwn.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof QuadrupleMapNodeData))
			return false;
		QuadrupleMapNodeData other = (QuadrupleMapNodeData) obj;
		if (propertiesInh == null) {
			if (other.propertiesInh != null)
				return false;
		} else if (!propertiesInh.equals(other.propertiesInh))
			return false;
		if (propertiesOwn == null) {
			if (other.propertiesOwn != null)
				return false;
		} else if (!propertiesOwn.equals(other.propertiesOwn))
			return false;
		if (valuesInh == null) {
			if (other.valuesInh != null)
				return false;
		} else if (!valuesInh.equals(other.valuesInh))
			return false;
		if (valuesOwn == null) {
			if (other.valuesOwn != null)
				return false;
		} else if (!valuesOwn.equals(other.valuesOwn))
			return false;
		return true;
	}
	
	
	
}
