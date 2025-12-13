/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.nextgenexample.preloading;

import com.example.nextgenexample.AdFragment;
import com.example.nextgenexample.databinding.FragmentPreloadMainBinding;

/** An [AdFragment] subclass that loads a native ad. */
public class PreloadMainFragment extends AdFragment<FragmentPreloadMainBinding> {
  // Default constructor required for fragment instantiation.
  public PreloadMainFragment() {}

  @Override
  protected BindingInflater<FragmentPreloadMainBinding> getBindingInflater() {
    return com.example.nextgenexample.databinding.FragmentPreloadMainBinding::inflate;
  }
}
