/*
 * (C) Copyright 2015-2016 Opencell SAS (http://opencellsoft.com/) and contributors.
 * (C) Copyright 2009-2014 Manaty SARL (http://manaty.net/) and contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 * This program is not suitable for any direct or indirect application in MILITARY industry
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.meveo.model.wf;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.meveo.model.*;
import org.meveo.model.crm.custom.CustomFieldValues;
import org.meveo.model.persistence.CustomFieldValuesConverter;

/**
 * @author Edward P. Legaspi | czetsuya@gmail.com
 * @version 6.9.0
 */
@Entity
@ModuleItem("Workflow")
@ModuleItemOrder(207)
@Cacheable
@ExportIdentifier({ "code"})
@CustomFieldEntity(cftCodePrefix = "WORKFLOW")
@Table(name = "wf_workflow", uniqueConstraints = @UniqueConstraint(columnNames = {"code" }))
@GenericGenerator(name = "ID_GENERATOR", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator", parameters = {@Parameter(name = "sequence_name", value = "wf_workflow_seq"), })
public class Workflow extends BusinessEntity implements ICustomFieldEntity {

	private static final long serialVersionUID = 1L;

	@Column(name = "wf_type", length = 255)
	@NotNull
    @Size(max = 255)
	String wfType = null;
	
	@OneToMany(mappedBy = "workflow", fetch = FetchType.LAZY, cascade = { CascadeType.REMOVE })
    @OrderBy("priority ASC")
	private List<WFTransition> transitions = new ArrayList<WFTransition>();

	@Type(type="numeric_boolean")
    @Column(name = "enable_hostory")
	private boolean enableHistory;

	@Column(name = "uuid", nullable = false, updatable = false, length = 60)
	@Size(max = 60)
	@NotNull
	private String uuid = UUID.randomUUID().toString();

	// @Type(type = "json")
	@Convert(converter = CustomFieldValuesConverter.class)
	@Column(name = "cf_values", columnDefinition = "text")
	private CustomFieldValues cfValues;

	/**
	 * @return the wfType
	 */
	public String getWfType() {
		return wfType;
	}

	/**
	 * @param wfType the wfType to set
	 */
	public void setWfType(String wfType) {
		this.wfType = wfType;
	}

	/**
	 * @return the transitions
	 */
	public List<WFTransition> getTransitions() {
		return transitions;
	}

	/**
	 * @param transitions the transitions to set
	 */
	public void setTransitions(List<WFTransition> transitions) {
		this.transitions = transitions;
	}

	/**
	 * @return the enbaleHistory
	 */
	public boolean isEnableHistory() {
		return enableHistory;
	}

	/**
	 * @param enbaleHistory the enbaleHistory to set
	 */
	public void setEnableHistory(boolean enbaleHistory) {
		this.enableHistory = enbaleHistory;
	}
		
	@Override
	public String toString() {
		return "Workflow [code=" + code + ", description=" + description + "]";
	}

	@Override
	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	@Override
	public String clearUuid() {
		String oldUuid = uuid;
		uuid = UUID.randomUUID().toString();
		return oldUuid;
	}

	@Override
	public ICustomFieldEntity[] getParentCFEntities() {
		return null;
	}

	@Override
	public CustomFieldValues getCfValues() {
		return cfValues;
	}

	public void setCfValues(CustomFieldValues cfValues) {
		this.cfValues = cfValues;
	}

	@Override
	public CustomFieldValues getCfValuesNullSafe() {
		if (cfValues == null) {
			cfValues = new CustomFieldValues();
		}
		return cfValues;
	}

	@Override
	public void clearCfValues() {
		cfValues = null;
	}
}
