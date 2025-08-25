/*
 * This file is part of "SAP Commerce Developers Toolset" plugin for IntelliJ IDEA.
 * Copyright (C) 2019-2025 EPAM Systems <hybrisideaplugin@epam.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package sap.commerce.toolset.typeSystem.settings.state

import com.intellij.util.xmlb.annotations.Tag

@Tag("Settings")
data class TSViewSettingsState(
    val showOnlyCustom: Boolean = false,
    val showMetaItems: Boolean = true,
    val showMetaRelations: Boolean = true,
    val showMetaEnums: Boolean = true,
    val showMetaCollections: Boolean = true,
    val showMetaAtomics: Boolean = true,
    val showMetaMaps: Boolean = true,
    val showMetaEnumValues: Boolean = true,
    val showMetaItemIndexes: Boolean = true,
    val showMetaItemAttributes: Boolean = true,
    val showMetaItemCustomProperties: Boolean = true,
    val groupItemByParent: Boolean = false,
)
