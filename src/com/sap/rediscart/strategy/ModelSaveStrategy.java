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
package com.sap.rediscart.strategy;

import de.hybris.platform.core.model.ItemModel;

import java.util.Collection;


/**
 * @author Henter Liu (henterji@163.com)
 */
public interface ModelSaveStrategy
{
	public boolean beforeSave(final Collection<? extends Object> toSave, ItemModel model);

	public boolean afterSave(ItemModel model);

	public boolean beforeRemove(final Collection<? extends Object> toSave, ItemModel model);

	public boolean afterRemove(ItemModel model);
}
