/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2007-2014 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2014 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.core.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * SyncTask
 *
 * @author brozow
 * @version $Id: $
 */
public class SyncTask extends AbstractTask {
	
	private static final Logger LOG = LoggerFactory.getLogger(SyncTask.class);
    
    private final Runnable m_action;

    private String m_preferredExecutor = TaskCoordinator.DEFAULT_EXECUTOR;
    
    /**
     * <p>Constructor for SyncTask.</p>
     *
     * @param coordinator a {@link org.opennms.core.tasks.TaskCoordinator} object.
     * @param parent a {@link org.opennms.core.tasks.ContainerTask} object.
     * @param action a {@link java.lang.Runnable} object.
     */
    public SyncTask(TaskCoordinator coordinator, ContainerTask<?> parent, Runnable action) {
        this(coordinator, parent, action, TaskCoordinator.DEFAULT_EXECUTOR);
    }


    /**
     * <p>Constructor for SyncTask.</p>
     *
     * @param coordinator a {@link org.opennms.core.tasks.TaskCoordinator} object.
     * @param parent a {@link org.opennms.core.tasks.ContainerTask} object.
     * @param action a {@link java.lang.Runnable} object.
     * @param preferredExecutor a {@link java.lang.String} object.
     */
    public SyncTask(TaskCoordinator coordinator, ContainerTask<?> parent, Runnable action, String preferredExecutor) {
        super(coordinator, parent);
        m_action = action;
        m_preferredExecutor = preferredExecutor;
    }

    /** {@inheritDoc} */
    @Override
    protected void doSubmit() {
        getCoordinator().submitToExecutor(getPreferredExecutor(), getRunnable(), this);
    }

    /**
     * This is the run method where the 'work' related to the Task gets down.  This method can be overridden
     * or a Runnable can be passed to the task in the constructor.  The Task is not complete until this method
     * finishes
     */
    private final void run() {
        if (m_action != null) {
            m_action.run();
        }
    }

    /**
     * This method is used by the TaskCoordinator to create runnable that will run this task
     */
    final Runnable getRunnable() {
        return new Runnable() {
          @Override
          public void run() {
              try {
                  SyncTask.this.run();
              } catch (Throwable t) {
                  LOG.error("Exception occurred executing task " + SyncTask.this, t);
              }
          }
          @Override
          public String toString() { return "Runner for "+SyncTask.this; }
        };
    }

    /**
     * <p>getPreferredExecutor</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getPreferredExecutor() {
        return m_preferredExecutor;
    }

    /**
     * <p>setPreferredExecutor</p>
     *
     * @param preferredExecutor a {@link java.lang.String} object.
     */
    public void setPreferredExecutor(String preferredExecutor) {
        m_preferredExecutor = preferredExecutor;
    }

    /**
     * <p>toString</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Override
    public String toString() {
        return m_action == null ? super.toString() : m_action.toString();
    }


}
