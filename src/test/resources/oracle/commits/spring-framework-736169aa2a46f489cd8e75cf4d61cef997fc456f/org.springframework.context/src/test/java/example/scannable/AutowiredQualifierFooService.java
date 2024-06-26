/*
 * Copyright 2002-2009 the original author or authors.
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

package example.scannable;

import java.util.concurrent.Future;
import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.AsyncResult;

/**
 * @author Mark Fisher
 * @author Juergen Hoeller
 */
public class AutowiredQualifierFooService implements FooService {

	@Autowired
	@Qualifier("testing")
	private FooDao fooDao;

	private boolean initCalled = false;

	@PostConstruct
	private void init() {
		if (this.initCalled) {
			throw new IllegalStateException("Init already called");
		}
		this.initCalled = true;
	}

	public String foo(int id) {
		return this.fooDao.findFoo(id);
	}

	public Future<String> asyncFoo(int id) {
		return new AsyncResult<String>(this.fooDao.findFoo(id));
	}

	public boolean isInitCalled() {
		return this.initCalled;
	}

}
