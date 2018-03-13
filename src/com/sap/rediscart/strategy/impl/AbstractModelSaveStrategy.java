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
package com.sap.rediscart.strategy.impl;

import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.servicelayer.model.ModelService;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;

import com.sap.rediscart.strategy.ModelSaveStrategy;
import com.sap.rediscart.util.RedisKeyGenerator;


/**
 * @author Henter Liu (henterji@163.com)
 */
public abstract class AbstractModelSaveStrategy implements ModelSaveStrategy
{
	private static final Logger LOG = LoggerFactory.getLogger(AbstractModelSaveStrategy.class);

	private ValueOperations<String, Object> valueOps;
	private SetOperations<String, Object> setOps;

	private RedisTemplate<String, Object> redisTemplate;
	private ModelService modelService;

	private RedisKeyGenerator redisKeyGenerator;

	@Override
	public boolean beforeSave(final Collection<? extends Object> toSave, final ItemModel model)
	{
		try
		{
			return doBeforeSave(toSave, model);
		}
		catch (final Exception e)
		{
			LOG.error("error occurs", e);
			return false;
		}
	}

	@Override
	public boolean afterSave(final ItemModel model)
	{
		try
		{
			return doAfterSave(model);
		}
		catch (final Exception e)
		{
			LOG.error("error occurs", e);
			return false;
		}
	}

	@Override
	public boolean beforeRemove(final Collection<? extends Object> toSave, final ItemModel model)
	{
		try
		{
			return doBeforeRemove(toSave, model);
		}
		catch (final Exception e)
		{
			LOG.error("error occurs", e);
			return false;
		}
	}

	@Override
	public boolean afterRemove(final ItemModel model)
	{
		try
		{
			return doAfterRemove(model);
		}
		catch (final Exception e)
		{
			LOG.error("error occurs", e);
			return false;
		}
	}

	protected abstract boolean doBeforeSave(final Collection<? extends Object> toSave, ItemModel model);

	protected abstract boolean doAfterSave(ItemModel model);

	protected abstract boolean doBeforeRemove(final Collection<? extends Object> toSave, ItemModel model);

	protected abstract boolean doAfterRemove(ItemModel model);

	/**
	 * @return the valueOps
	 */
	public ValueOperations<String, Object> getValueOps()
	{
		return valueOps;
	}

	/**
	 * @return the setOps
	 */
	public SetOperations<String, Object> getSetOps()
	{
		return setOps;
	}

	/**
	 * @return the redisTemplate
	 */
	public RedisTemplate<String, Object> getRedisTemplate()
	{
		return redisTemplate;
	}

	/**
	 * @param redisTemplate
	 *           the redisTemplate to set
	 */
	public void setRedisTemplate(final RedisTemplate<String, Object> redisTemplate)
	{
		this.redisTemplate = redisTemplate;
		this.setOps = this.redisTemplate.opsForSet();
		this.valueOps = this.redisTemplate.opsForValue();
	}

	/**
	 * @return the modelService
	 */
	public ModelService getModelService()
	{
		return modelService;
	}

	/**
	 * @param modelService
	 *           the modelService to set
	 */
	public void setModelService(final ModelService modelService)
	{
		this.modelService = modelService;
	}

	/**
	 * @return the redisKeyGenerator
	 */
	public RedisKeyGenerator getRedisKeyGenerator()
	{
		return redisKeyGenerator;
	}

	/**
	 * @param redisKeyGenerator
	 *           the redisKeyGenerator to set
	 */
	public void setRedisKeyGenerator(final RedisKeyGenerator redisKeyGenerator)
	{
		this.redisKeyGenerator = redisKeyGenerator;
	}
}
