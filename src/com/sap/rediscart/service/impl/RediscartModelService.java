/*
 * Copyright [2018] [Henter Liu]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sap.rediscart.service.impl;

import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNull;

import de.hybris.platform.core.PK;
import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.servicelayer.exceptions.ModelRemovalException;
import de.hybris.platform.servicelayer.exceptions.ModelSavingException;
import de.hybris.platform.servicelayer.internal.model.impl.DefaultModelService;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.rediscart.strategy.ModelSaveStrategy;


/**
 * @author Henter Liu (henterji@163.com)
 */
public class RediscartModelService extends DefaultModelService
{
	@SuppressWarnings("unused")
	private static final Logger LOG = LoggerFactory.getLogger(RediscartModelService.class);

	private Map<String, ModelSaveStrategy> modelSaveHandlers;

	@Override
	public void save(final Object model)
	{
		validateParameterNotNull(model, "Parameter 'model' is null!");
		doSaveAll(Collections.singletonList(model));
	}

	@Override
	public void saveAll() throws ModelSavingException
	{
		final Set<Object> objs = prepareObjectsToSave();
		doSaveAll(objs);
	}

	@Override
	public void saveAll(final Object... models) throws ModelSavingException
	{
		doSaveAll(Arrays.asList(models));
	}

	@Override
	public void saveAll(final Collection<? extends Object> models) throws ModelSavingException
	{
		doSaveAll(models);
	}

	/**
	 * The real save method
	 */
	private void doSaveAll(final Collection<? extends Object> models) throws ModelSavingException
	{
		final Set<Object> toSave = new HashSet<Object>(models);

		beforeSave(toSave, models);

		if (toSave.isEmpty())
		{
			return; // nothing to save
		}
		super.saveAll(toSave);

		afterSave(toSave);
	}

	private void beforeSave(final Collection<? extends Object> toSave, final Collection<? extends Object> models)
	{
		for (final Object model : models)
		{
			if (model instanceof ItemModel)
			{
				final ModelSaveStrategy modelSaveStrategy = modelSaveHandlers.get(((ItemModel) model).getItemtype());
				if (modelSaveStrategy != null)
				{
					if (modelSaveStrategy.beforeSave(toSave, (ItemModel) model))
					{
						// TODO: do something to interrupt saving
						// toSave.remove(model);
					}
				}
				else
				{
					// LOG.debug("didn't find save strategy for '" + ((ItemModel) model).getItemtype() + "'");
				}
			}
		}
	}

	private void afterSave(final Collection<? extends Object> savedModels)
	{
		for (final Object model : savedModels)
		{
			if (model instanceof ItemModel)
			{
				final ModelSaveStrategy modelSaveStrategy = modelSaveHandlers.get(((ItemModel) model).getItemtype());
				if (modelSaveStrategy != null)
				{
					modelSaveStrategy.afterSave((ItemModel) model);
				}
				else
				{
					// LOG.debug("didn't find save strategy for '" + ((ItemModel) model).getItemtype() + "'");
				}
			}
		}
	}

	/**
	 * Prepares objects to save.
	 */
	private Set<Object> prepareObjectsToSave()
	{
		final Set<Object> newOnes = getModelContext().getNew();
		final Set<Object> modifiedOnes = getModelContext().getModified();

		final Set<Object> toSave;
		if (newOnes.isEmpty())
		{
			toSave = modifiedOnes;
		}
		else if (modifiedOnes.isEmpty())
		{
			toSave = newOnes;
		}
		else
		{
			toSave = new LinkedHashSet<Object>(newOnes.size() + modifiedOnes.size());
			toSave.addAll(newOnes);
			toSave.addAll(modifiedOnes);
		}
		return toSave;
	}

	@Override
	public void remove(final Object model) throws ModelRemovalException
	{
		removeAll(Collections.singleton(model));
	}

	@Override
	public void remove(final PK pk) throws ModelRemovalException
	{
		remove(this.<Object> get(pk));
	}

	@Override
	public void removeAll(final Collection<? extends Object> models) throws ModelRemovalException
	{
		doRemoveAll(models);
	}

	@Override
	public void removeAll(final Object... models) throws ModelRemovalException
	{
		removeAll(Arrays.asList(models));
	}

	private void doRemoveAll(final Collection<? extends Object> models) throws ModelRemovalException
	{
		final Set<Object> toRemove = new HashSet<Object>(models);

		beforeRemove(toRemove, models);

		if (toRemove.isEmpty())
		{
			return; //nothing to save
		}
		super.removeAll(models);

		afterRemove(toRemove);
	}

	private void beforeRemove(final Collection<? extends Object> toSave, final Collection<? extends Object> models)
	{
		for (final Object model : models)
		{
			if (model instanceof ItemModel)
			{
				final ModelSaveStrategy modelSaveStrategy = modelSaveHandlers.get(((ItemModel) model).getItemtype());
				if (modelSaveStrategy != null)
				{
					if (modelSaveStrategy.beforeRemove(toSave, (ItemModel) model))
					{
						//TODO: do something to interrupt saving
						//toSave.remove(model);
					}
				}
				else
				{
					// LOG.debug("didn't find save strategy for '" + ((ItemModel) model).getItemtype() + "'");
				}
			}
		}
	}

	private void afterRemove(final Collection<? extends Object> savedModels)
	{
		for (final Object model : savedModels)
		{
			if (model instanceof ItemModel)
			{
				final ModelSaveStrategy modelSaveStrategy = modelSaveHandlers.get(((ItemModel) model).getItemtype());
				if (modelSaveStrategy != null)
				{
					modelSaveStrategy.afterRemove((ItemModel) model);
				}
				else
				{
					// LOG.debug("didn't find save strategy for '" + ((ItemModel) model).getItemtype() + "'");
				}
			}
		}
	}

	/**
	 * @return the modelSaveHandlers
	 */
	public Map<String, ModelSaveStrategy> getModelSaveHandlers()
	{
		return modelSaveHandlers;
	}

	/**
	 * @param modelSaveHandlers
	 *           the modelSaveHandlers to set
	 */
	public void setModelSaveHandlers(final Map<String, ModelSaveStrategy> modelSaveHandlers)
	{
		this.modelSaveHandlers = modelSaveHandlers;
	}
}
