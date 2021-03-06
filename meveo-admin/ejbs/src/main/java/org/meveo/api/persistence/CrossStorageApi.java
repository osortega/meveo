/**
 * 
 */
package org.meveo.api.persistence;

import java.io.IOException;
import java.util.Map;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.meveo.admin.exception.BusinessException;
import org.meveo.api.exception.BusinessApiException;
import org.meveo.api.exception.EntityDoesNotExistsException;
import org.meveo.cache.CustomFieldsCacheContainerProvider;
import org.meveo.model.customEntities.CustomEntityInstance;
import org.meveo.model.customEntities.CustomEntityTemplate;
import org.meveo.model.persistence.CEIUtils;
import org.meveo.model.persistence.JacksonUtil;
import org.meveo.model.storage.Repository;
import org.meveo.persistence.CrossStorageService;

/**
 * The Class CrossStorageApi.
 *
 * @author clement.bareth
 * @version 6.11.0
 * @since 6.8.0
 */
// @Stateless
public class CrossStorageApi{

	@Inject
	private CrossStorageService crossStorageService;
	
	@Inject
	private CustomFieldsCacheContainerProvider cache;
	
	public <T> CrossStorageRequest<T> find(Repository repository, Class<T> cetClass) {
		return new CrossStorageRequest(repository, crossStorageService, cetClass, getCet(cetClass));
	}
	
	/**
	 * Find an instance of a given CET
	 *
	 * @param repository the repository where the instance is stored
	 * @param uuid       the uuid of the instance
	 * @param cetClass   the clazz of the cet's type
	 * @return the instanc of the cet
	 * @throws EntityDoesNotExistsException the entity does not exists exception
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public <T> T find(Repository repository, String uuid, Class<T> cetClass) throws EntityDoesNotExistsException {
		CustomEntityTemplate cet = getCet(cetClass);
		Map<String, Object> values = crossStorageService.find(repository, cet, uuid, true);
		return JacksonUtil.convert(values, cetClass);
	}
	
	/**
	 * Find an instance of a given CET
	 *
	 * @param repository the repository where the instance is stored
	 * @param uuid       the uuid of the instance
	 * @param cetCode    the code of the ce
	 * @return the instanc of the cet
	 * @throws EntityDoesNotExistsException the entity does not exists exception
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public CustomEntityInstance find(Repository repository, String uuid, String cetCode) throws EntityDoesNotExistsException {
		CustomEntityTemplate cet = cache.getCustomEntityTemplate(cetCode);
		Map<String, Object> values = crossStorageService.find(repository, cet, uuid, true);
		var cei = CEIUtils.pojoToCei(values);
		cei.setCetCode(cetCode);
		cei.setCet(cet);
		return cei;
	}

	/**
	 * 
	 * @param repository the repository where to save data
	 * @param value      the data to save
	 * @return the UUID of the created / updated data
	 * @throws BusinessApiException         See {@link CrossStorageService#createOrUpdate(Repository, org.meveo.model.customEntities.CustomEntityInstance)}
	 * @throws EntityDoesNotExistsException See {@link CrossStorageService#createOrUpdate(Repository, org.meveo.model.customEntities.CustomEntityInstance)}
	 * @throws BusinessException            See {@link CrossStorageService#createOrUpdate(Repository, org.meveo.model.customEntities.CustomEntityInstance)}
	 * @throws IOException                  See {@link CrossStorageService#createOrUpdate(Repository, org.meveo.model.customEntities.CustomEntityInstance)}
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public String createOrUpdate(Repository repository, Object value) throws BusinessApiException, EntityDoesNotExistsException, BusinessException, IOException {
		CustomEntityInstance cei = value instanceof CustomEntityInstance
				? (CustomEntityInstance) value 
				: CEIUtils.pojoToCei(value);
				
		var result = crossStorageService.createOrUpdate(repository, cei);
		return result.getBaseEntityUuid();
	}
	
	/**
	 * Remove an instance of a given CET
	 *
	 * @param repository the repository where the instance is stored
	 * @param uuid       the uuid of the instance
	 * @param cetCode 	 code of the cet
	 * @throws BusinessException if error occurs
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void remove(Repository repository, String uuid, String cetCode) throws BusinessException {
		crossStorageService.remove(repository, cache.getCustomEntityTemplate(cetCode), uuid);
	}
	
	/**
	 * Remove an instance of a given CET
	 *
	 * @param repository the repository where the instance is stored
	 * @param uuid       the uuid of the instance
	 * @param cetClass   the clazz of the cet's type
	 * @throws BusinessException if error occurs
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void remove(Repository repository, String uuid, Class<?> cetClass) throws BusinessException {
		crossStorageService.remove(repository, getCet(cetClass), uuid);
	}
	
	/**
	 * @param cetClass
	 * @return
	 * @throws IllegalArgumentException
	 */
	private CustomEntityTemplate getCet(Class<?> cetClass) throws IllegalArgumentException {
		CustomEntityTemplate cet = cache.getCustomEntityTemplate(cetClass.getSimpleName());
		if (cet == null) {
			throw new IllegalArgumentException("CET " + cetClass.getSimpleName() + " does not exists");
		}
		return cet;
	}

}
