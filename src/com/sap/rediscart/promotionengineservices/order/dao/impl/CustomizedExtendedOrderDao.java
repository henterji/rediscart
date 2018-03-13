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
package com.sap.rediscart.promotionengineservices.order.dao.impl;

import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.promotionengineservices.order.dao.impl.DefaultExtendedOrderDao;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.user.UserService;

import org.apache.commons.lang.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.sap.rediscart.model.order.RedisCartModel;
import com.sap.rediscart.util.RedisKeyGenerator;


/**
 * @author Henter Liu (henterji@163.com)
 */
public class CustomizedExtendedOrderDao extends DefaultExtendedOrderDao
{
	private ValueOperations<String, Object> valueOps;
	private RedisTemplate<String, Object> redisTemplate;

	private ModelService modelService;
	private UserService userService;
	private RedisKeyGenerator redisKeyGenerator;

	/*
	 * (non-Javadoc)
	 *
	 * @see de.hybris.platform.promotionengineservices.order.dao.ExtendedOrderDao#findOrderByCode(java.lang.String)
	 */
	@Override
	public AbstractOrderModel findOrderByCode(final String code)
	{
		if (StringUtils.isBlank(code))
		{
			return null;
		}
		final String cartKey = getRedisKeyGenerator().generateCartKey(code);
		final Object value = valueOps.get(cartKey);
		if (value == null)
		{
			return super.findOrderByCode(code);
		}
		final RedisCartModel redisCart = getModelService().get(value);
		return redisCart;
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
		this.valueOps = this.redisTemplate.opsForValue();
	}

	/**
	 * @return the modelService
	 */
	@Override
	public ModelService getModelService()
	{
		return modelService;
	}

	/**
	 * @param modelService
	 *           the modelService to set
	 */
	@Override
	public void setModelService(final ModelService modelService)
	{
		this.modelService = modelService;
	}

	/**
	 * @return the userService
	 */
	public UserService getUserService()
	{
		return userService;
	}

	/**
	 * @param userService
	 *           the userService to set
	 */
	public void setUserService(final UserService userService)
	{
		this.userService = userService;
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
