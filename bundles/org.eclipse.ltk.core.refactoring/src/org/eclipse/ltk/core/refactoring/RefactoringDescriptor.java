/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sergey Prigogin <eclipse.sprigogin@gmail.com> - [refactoring] Provide a way to implement refactorings that depend on resources that have to be explicitly released - https://bugs.eclipse.org/347599
 *******************************************************************************/
package org.eclipse.ltk.core.refactoring;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.ltk.core.refactoring.history.IRefactoringExecutionListener;
import org.eclipse.ltk.core.refactoring.history.IRefactoringHistoryListener;
import org.eclipse.ltk.core.refactoring.history.IRefactoringHistoryService;

/**
 * Descriptor object of a refactoring.
 * <p>
 * A refactoring descriptor contains refactoring-specific data which allows the
 * framework to completely reconstruct a particular refactoring instance and
 * execute it on an arbitrary workspace.
 * </p>
 * <p>
 * Refactoring descriptors contain the following information:
 * </p>
 * <ul>
 * <li>a short description string, which provides a human-readable text
 * designed to be displayed in the user interface to represent the refactoring
 * in trees and lists. Descriptions are automatically generated by refactorings.</li>
 * <li> an optional comment string, which provides a full human-readable
 * description of the refactoring. Comments are automatically generated by
 * refactorings and provide more refactoring-specific information, such as which
 * elements have participated in the refactoring. </li>
 * <li> refactoring descriptor flags, which tell the framework what capabilities
 * or properties a certain refactorings has when executed in a remote context.
 * </li>
 * <li> a timestamp, measured as the milliseconds since January 1, 1970,
 * 00:00:00 GMT, which denotes the original execution time of the refactoring.
 * </li>
 * <li> a unique ID, which denotes a certain kind of refactoring (e.g. Rename
 * File). This ID is usually composed of the plugin identifier of the
 * contributing plugin and a plugin-wide unique identifier (e.g.
 * <code>org.eclipse.ltk.core.refactoring.renameFile</code>). </li>
 * <li> the optional name of the project this refactoring is associated with.
 * Note that the project name is not available if the refactoring cannot be
 * associated with a single project, or the refactoring descriptor has been read
 * from a file which cannot be associated with a project. </li>
 * </ul>
 * <p>
 * Refactoring descriptors are identified by their refactoring id
 * {@link #getID()} and their time stamps {@link #getTimeStamp()} and are
 * potentially heavy weight objects which should not be held on to. Use
 * refactoring descriptor proxies {@link RefactoringDescriptorProxy} to present
 * refactoring descriptors in the user interface or otherwise manipulate
 * refactoring histories.
 * </p>
 * <p>
 * Clients which create specific refactoring descriptors during change
 * generation should choose a short, informative and human-readable description
 * of the particular refactoring instance and pass appropriate descriptor flags
 * to the constructor. More details about a particular refactoring can be
 * revealed in the comment, which contains more text with refactoring-specific
 * information.
 * </p>
 * <p>
 * Refactoring descriptors do not provide version information. It is the
 * responsibility of the client to enhance subclasses with refactoring version
 * information in order to provide a means of schema evolution.
 * </p>
 * <p>
 * All time stamps are measured as the milliseconds since January 1, 1970,
 * 00:00:00 GMT.
 * </p>
 * <p>
 * Note: this class is indented to be subclassed by clients to provide
 * specialized refactoring descriptors for particular refactorings.
 * </p>
 *
 * @see RefactoringDescriptorProxy
 * @see IRefactoringHistoryService
 *
 * @since 3.2
 */
public abstract class RefactoringDescriptor implements Comparable<RefactoringDescriptor> {

	/**
	 * Constant describing the API change flag (value: <code>1</code>).
	 * <p>
	 * Clients should set this flag to indicate that the represented refactoring
	 * may cause breaking API changes. If clients set the
	 * {@link #BREAKING_CHANGE} flag, they should set {@link #STRUCTURAL_CHANGE}
	 * as well. Typically, refactorings which change elements that are marked as
	 * API according to the semantics of the associated programming language
	 * should set this flag. This flag is used by the refactoring framework to
	 * determine whether a refactoring may break existing API when replayed by
	 * clients.
	 * </p>
	 */
	public static final int BREAKING_CHANGE= 1 << 0;

	/**
	 * The unknown refactoring id (value:
	 * <code>org.eclipse.ltk.core.refactoring.unknown</code>).
	 * <p>
	 * This id is reserved by the refactoring framework to signal that a
	 * refactoring has been performed which did not deliver a refactoring
	 * descriptor via its {@link Change#getDescriptor()} method. The refactoring
	 * history service never returns unknown refactorings. For consistency
	 * reasons, they are reported for {@link IRefactoringExecutionListener} or
	 * {@link IRefactoringHistoryListener} in order to keep clients of these
	 * listeners synchronized with the workbench's operation history.
	 * </p>
	 */
	public static final String ID_UNKNOWN= "org.eclipse.ltk.core.refactoring.unknown"; //$NON-NLS-1$

	/**
	 * Constant describing the multi change flag (value: <code>4</code>).
	 * <p>
	 * Clients should set this flag to indicate that the change created by the
	 * represented refactoring might causes changes in other files than the
	 * files of the input elements according to the semantics of the associated
	 * programming language. Typically, refactorings which update references to
	 * the refactored element should set this flag. This flag is used during
	 * team synchronize operations to optimize the processing of refactorings.
	 * </p>
	 */
	public static final int MULTI_CHANGE= 1 << 2;

	/** Constant describing the absence of any flags (value: <code>0</code>). */
	public static final int NONE= 0;

	/**
	 * Constant describing the structural change flag (value: <code>2</code>).
	 * <p>
	 * Clients should set this flag to indicate that the change created by the
	 * represented refactoring might be a structural change according to the
	 * semantics of the associated programming language. Typically, refactorings
	 * which cause changes in elements other than the element which declares the
	 * refactored element should set this flag. This flag is used by
	 * language-specific tools to determine whether the refactoring may impact
	 * client code.
	 * </p>
	 */
	public static final int STRUCTURAL_CHANGE= 1 << 1;

	/**
	 * Constant describing the user flag (value: <code>256</code>).
	 * <p>
	 * This constant is not intended to be used in refactoring descriptors.
	 * Clients should use the value of this constant to define user-defined
	 * flags with integer values greater than this constant. Clients must not
	 * use this constant directly.
	 * </p>
	 */
	public static final int USER_CHANGE= 1 << 8;

	/** The comment of the refactoring, or <code>null</code> for no comment */
	private String fComment;

	/** The non-empty description of the refactoring */
	private String fDescription;

	/** The flags of the refactoring, or <code>NONE</code> */
	private int fFlags;

	/** The project name, or <code>null</code> for no project */
	private String fProject;

	/** The unique id of the refactoring */
	private final String fRefactoringId;

	/**
	 * The time stamp, or <code>-1</code> if no time information is associated
	 * with the refactoring
	 */
	private long fTimeStamp= -1;

	/**
	 * Creates a new refactoring descriptor.
	 *
	 * @param id
	 *            the unique id of the refactoring
	 * @param project
	 *            the non-empty name of the project associated with this
	 *            refactoring, or <code>null</code> for a workspace
	 *            refactoring
	 * @param description
	 *            a non-empty human-readable description of the particular
	 *            refactoring instance
	 * @param comment
	 *            the human-readable comment of the particular refactoring
	 *            instance, or <code>null</code> for no comment
	 * @param flags
	 *            the flags of the refactoring descriptor
	 */
	protected RefactoringDescriptor(final String id, final String project, final String description, final String comment, final int flags) {
		Assert.isNotNull(id);
		Assert.isLegal(!"".equals(id), "Refactoring id must not be empty"); //$NON-NLS-1$ //$NON-NLS-2$
		Assert.isLegal(project == null || !"".equals(project), "Project must either be null or non-empty"); //$NON-NLS-1$ //$NON-NLS-2$
		Assert.isNotNull(description);
		Assert.isLegal(!"".equals(description), "Description must not be empty"); //$NON-NLS-1$//$NON-NLS-2$
		Assert.isLegal(flags >= NONE, "Flags must be non-negative"); //$NON-NLS-1$
		fRefactoringId= id;
		fDescription= description;
		fProject= project;
		fComment= comment;
		fFlags= flags;
	}

	/**
	 * {@inheritDoc}
	 * @since 3.7
	 */
	@Override
	public final int compareTo(final RefactoringDescriptor descriptor) {
		long delta= fTimeStamp - descriptor.fTimeStamp;
		if (delta < 0)
			return -1;
		else if (delta > 0)
			return +1;
		return 0;
	}

	/**
	 * Creates the a new refactoring instance for this refactoring descriptor.
	 * <p>
	 * The returned refactoring must be in an initialized state, i.e. ready to
	 * be executed via {@link PerformRefactoringOperation}.
	 * </p>
	 * <p>
	 * This method is not intended to be called directly from code that does not belong to this
	 * class and its subclasses. External code should call
	 * {@link #createRefactoringContext(RefactoringStatus)} and obtain the refactoring object from
	 * the refactoring context.
	 * </p>
	 *
	 * @param status
	 *          a refactoring status used to describe the outcome of the initialization
	 * @return the refactoring, or <code>null</code> if this refactoring
	 *         descriptor represents the unknown refactoring, or if no
	 *         refactoring contribution is available for this refactoring
	 *         descriptor which is capable to create a refactoring
	 * @throws CoreException
	 *         if an error occurs while creating the refactoring instance
	 */
	public abstract Refactoring createRefactoring(RefactoringStatus status) throws CoreException;

	/**
	 * Creates the a new refactoring context and the associated refactoring instance for this
	 * refactoring descriptor.
	 * <p>
	 * This method is used by the refactoring framework to instantiate a refactoring
	 * from a refactoring descriptor, in order to apply it later on a local or remote workspace.
	 * </p>
	 * <p>
	 * The default implementation of this method wraps the refactoring in a trivial refactoring
	 * context. Subclasses may override this method to create a custom refactoring context.
	 * </p>
	 *
	 * @param status
	 * 			a refactoring status used to describe the outcome of the initialization
	 * @return the refactoring context, or <code>null</code> if this refactoring
	 *         	descriptor represents the unknown refactoring, or if no
	 *         	refactoring contribution is available for this refactoring
	 *         	descriptor which is capable to create a refactoring.
	 * @throws CoreException
	 *          if an error occurs while creating the refactoring context
	 * @since 3.6
	 */
	public RefactoringContext createRefactoringContext(RefactoringStatus status) throws CoreException {
		Refactoring refactoring= createRefactoring(status);
		if (refactoring == null)
			return null;
		return new RefactoringContext(refactoring);
	}

	@Override
	public final boolean equals(final Object object) {
		if (object instanceof RefactoringDescriptor) {
			final RefactoringDescriptor descriptor= (RefactoringDescriptor) object;
			return fTimeStamp == descriptor.fTimeStamp && getDescription().equals(descriptor.getDescription());
		}
		return false;
	}

	/**
	 * Returns the details comment.
	 * <p>
	 * This information is used in the user interface to show additional details
	 * about the performed refactoring.
	 * </p>
	 *
	 * @return the details comment, or the empty string
	 */
	public final String getComment() {
		return (fComment != null) ? fComment : ""; //$NON-NLS-1$
	}

	/**
	 * Returns the description.
	 * <p>
	 * This information is used to label a refactoring in the user interface.
	 * </p>
	 *
	 * @return the description
	 */
	public final String getDescription() {
		return fDescription;
	}

	/**
	 * Returns the flags.
	 *
	 * @return the flags
	 */
	public final int getFlags() {
		return fFlags;
	}

	/**
	 * Returns the refactoring id.
	 *
	 * @return the refactoring id.
	 */
	public final String getID() {
		return fRefactoringId;
	}

	/**
	 * Returns the project name.
	 *
	 * @return the non-empty name of the project, or <code>null</code>
	 */
	public final String getProject() {
		return fProject;
	}

	/**
	 * Returns the time stamp.
	 *
	 * @return the time stamp, or <code>-1</code> if no time information is
	 *         available
	 */
	public final long getTimeStamp() {
		return fTimeStamp;
	}

	@Override
	public final int hashCode() {
		int code= getDescription().hashCode();
		if (fTimeStamp >= 0)
			code+= 17 * Long.hashCode(fTimeStamp);
		return code;
	}

	/**
	 * Sets the details comment of this refactoring.
	 * <p>
	 * Note: This API must not be extended or reimplemented and should not be
	 * called from outside the refactoring framework.
	 * </p>
	 *
	 * @param comment
	 *            the comment to set, or <code>null</code> for no comment
	 */
	public void setComment(final String comment) {
		fComment= comment;
	}

	/**
	 * Sets the description of this refactoring.
	 * <p>
	 * Note: This API must not be extended or reimplemented and should not be
	 * called from outside the refactoring framework.
	 * </p>
	 *
	 * @param description
	 *            the non-empty description of the refactoring to set
	 *
	 * @since 3.3
	 */
	public void setDescription(final String description) {
		Assert.isNotNull(description);
		Assert.isLegal(!"".equals(description), "Description must not be empty"); //$NON-NLS-1$ //$NON-NLS-2$
		fDescription= description;
	}

	/**
	 * Sets the flags of this refactoring.
	 * <p>
	 * Note: This API must not be extended or reimplemented and should not be
	 * called from outside the refactoring framework.
	 * </p>
	 *
	 * @param flags
	 *            the flags to set, or <code>NONE</code> to clear the flags
	 *
	 * @since 3.3
	 */
	public void setFlags(final int flags) {
		Assert.isLegal(flags >= NONE, "Flags must be non-negative"); //$NON-NLS-1$
		fFlags= flags;
	}

	/**
	 * Sets the project name of this refactoring.
	 * <p>
	 * Note: This API must not be extended or reimplemented and should not be
	 * called from outside the refactoring framework.
	 * </p>
	 *
	 * @param project
	 *            the non-empty project name to set, or <code>null</code> for
	 *            the workspace
	 */
	public void setProject(final String project) {
		Assert.isLegal(project == null || !"".equals(project), "Project must either be null or non-empty"); //$NON-NLS-1$ //$NON-NLS-2$
		fProject= project;
	}

	/**
	 * Sets the time stamp of this refactoring. This method can be called only
	 * once.
	 * <p>
	 * Note: This API must not be extended or reimplemented and should not be
	 * called from outside the refactoring framework.
	 * </p>
	 *
	 * @param stamp
	 *            the time stamp to set
	 */
	public void setTimeStamp(final long stamp) {
		Assert.isTrue(stamp >= 0);
		fTimeStamp= stamp;
	}

	@Override
	public String toString() {

		final StringBuilder buffer= new StringBuilder(128);

		buffer.append(getClass().getName());
		if (ID_UNKNOWN.equals(fRefactoringId))
			buffer.append("[unknown refactoring]"); //$NON-NLS-1$
		else {
			buffer.append("[timeStamp="); //$NON-NLS-1$
			buffer.append(fTimeStamp);
			buffer.append(",id="); //$NON-NLS-1$
			buffer.append(fRefactoringId);
			buffer.append(",description="); //$NON-NLS-1$
			buffer.append(fDescription);
			buffer.append(",project="); //$NON-NLS-1$
			buffer.append(fProject);
			buffer.append(",comment="); //$NON-NLS-1$
			buffer.append(fComment);
			buffer.append(",flags="); //$NON-NLS-1$
			buffer.append(fFlags);
			buffer.append("]"); //$NON-NLS-1$
		}

		return buffer.toString();
	}
}
