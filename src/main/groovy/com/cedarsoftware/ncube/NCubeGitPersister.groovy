package com.cedarsoftware.ncube

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ListBranchCommand
import org.eclipse.jgit.lib.AnyObjectId
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevTree
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.treewalk.TreeWalk

/**
 * NCube Persister implementation that stores / retrieves n-cubes directly from
 * a Git repository.
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br/>
 *         Copyright (c) Cedar Software LLC
 *         <br/><br/>
 *         Licensed under the Apache License, Version 2.0 (the 'License');
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br/><br/>
 *         http://www.apache.org/licenses/LICENSE-2.0
 *         <br/><br/>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an 'AS IS' BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
class NCubeGitPersister implements NCubeReadOnlyPersister
{
    Repository repo;

    void setRepositoryDir(String dir)
    {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        repo = builder.setGitDir(new File(dir))
                .readEnvironment() // scan environment GIT_* variables
                .findGitDir() // scan up the file system tree
                .build();

        println repo
        println '--------------------------------------------'
        ObjectId lastCommitId = repo.resolve(Constants.HEAD);
//        listAllCommits()
//        println '------------------------'
        listAllBranches()
        println '------------------------'
        listAllTags()
        println '------------------------'
        listRepositoryContents(lastCommitId)
    }

    private void listRepositoryContents(AnyObjectId root) throws IOException
    {
        // a RevWalk allows to walk over commits based on some filtering that is defined
        RevWalk walk = new RevWalk(repo)

        RevCommit commit = walk.parseCommit(root)
        RevTree tree = commit.tree

        // now use a TreeWalk to iterate over all files in the Tree recursively
        // you can set Filters to narrow down the results if needed
        TreeWalk treeWalk = new TreeWalk(repo)
        treeWalk.addTree(tree)
        treeWalk.recursive = true
        while (treeWalk.next())
        {
            println(treeWalk.pathString)
        }
    }

    private void listAllCommits()
    {
        Git git = new Git(repo)
        Iterable<RevCommit> commits = git.log().all().call()
        int count = 0;
        for (RevCommit commit : commits)
        {
            println('LogCommit: ' + commit)
            count++
        }
        println(count)
    }

    private void listAllBranches()
    {
        println('Listing local branches:')
        List<Ref> call = new Git(repo).branchList().call();
        for (Ref ref : call)
        {
            println('Branch: ' + ref + ' ' + ref.name + ' ' + ref.objectId.name)
        }

        println('Now including remote branches:');
        call = new Git(repo).branchList().setListMode(ListBranchCommand.ListMode.ALL).call();
        for (Ref ref : call)
        {
            println('Branch: ' + ref + ' ' + ref.name + ' ' + ref.objectId.name);
        }
    }

    private void listAllTags()
    {
        List<Ref> call = new Git(repo).tagList().call()
        for (Ref ref : call)
        {
            ref.
            println('Tag: ' + ref.name + ' ' + ref.objectId.name)
        }
    }

    NCube loadCube(NCubeInfoDto cubeInfo)
    {
        return null
    }

    Object[] getCubeRecords(ApplicationID appId, String pattern)
    {
        return new Object[0]
    }

    Object[] getAppNames(String tenant)
    {
        return new Object[0]
    }

    Object[] getAppVersions(ApplicationID appId)
    {
        return new Object[0]
    }

    boolean doesCubeExist(ApplicationID appId, String cubeName)
    {
        return false
    }

    Object[] getDeletedCubeRecords(ApplicationID appId, String pattern)
    {
        return new Object[0]
    }

    Object[] getRevisions(ApplicationID appId, String cubeName)
    {
        return new Object[0]
    }

    String getNotes(ApplicationID appId, String cubeName)
    {
        return null
    }

    String getTestData(ApplicationID appId, String cubeName)
    {
        return null
    }
}
