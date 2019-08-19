/*
 * Copyright (c) 2012 - 2018 Arvato Systems GmbH
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
package com.arvatosystems.t9t.tfi.component;

import java.util.ArrayList;
import java.util.List;

import com.arvatosystems.t9t.translation.be.importer.SupportedLanguagesImporter;

public class Dropdown28LanguageCode extends Dropdown28Ext {

    private static final long serialVersionUID = 7804881425211020007L;

    private static final List<String> myModelData = new ArrayList<>(new SupportedLanguagesImporter().readSupportedLanguages());

    public Dropdown28LanguageCode() {
        super(myModelData);
        this.setMaxlength(8);
    }
}
