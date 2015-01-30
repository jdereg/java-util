package com.cedarsoftware.ncube

import com.cedarsoftware.util.IOUtilities
import com.cedarsoftware.util.StringUtilities
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ListBranchCommand
import org.eclipse.jgit.errors.IncorrectObjectTypeException
import org.eclipse.jgit.errors.MissingObjectException
import org.eclipse.jgit.lib.AnyObjectId
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.ObjectLoader
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevTree
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.treewalk.TreeWalk
import org.eclipse.jgit.treewalk.filter.TreeFilter

import java.util.regex.Matcher

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
class NCubeGitPersister implements NCubePersister, NCubeReadOnlyPersister
{
    Repository repository;
    String repoDir;

    private Repository getRepoByAppId(ApplicationID appId)
    {
        return repository;
    }

    void setRepositoryDir(String dir)
    {
        repoDir = dir;
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        repository = builder.setGitDir(new File(dir))
                .readEnvironment() // scan environment GIT_* variables
                .findGitDir() // scan up the file system tree
                .build();
//
//        println repository
//        println '--------------------------------------------'
//        ObjectId lastCommitId = repository.resolve(Constants.HEAD);
////        listAllCommits()
////        println '------------------------'
//        listAllBranches()
//        println '------------------------'
//        listAllTags()
//        println '------------------------'
//        listRepositoryContents(lastCommitId)
    }

    private void listRepositoryContents(AnyObjectId root) throws IOException
    {
        // a RevWalk allows to walk over commits based on some filtering that is defined
        RevWalk walk = new RevWalk(repository)

        RevCommit commit = walk.parseCommit(root)
        RevTree tree = commit.tree

        // now use a TreeWalk to iterate over all files in the Tree recursively
        // you can set Filters to narrow down the results if needed
        TreeWalk treeWalk = new TreeWalk(repository)
        treeWalk.addTree(tree)
        treeWalk.recursive = true
        while (treeWalk.next())
        {
            println(treeWalk.pathString)
        }
    }

    private void listAllCommits()
    {
        Git git = new Git(repository)
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
        List<Ref> call = new Git(repository).branchList().call();
        for (Ref ref : call)
        {
            println('Branch: ' + ref + ' ' + ref.name + ' ' + ref.objectId.name)
        }

        println('Now including remote branches:');
        call = new Git(repository).branchList().setListMode(ListBranchCommand.ListMode.ALL).call();
        for (Ref ref : call)
        {
            println('Branch: ' + ref + ' ' + ref.name + ' ' + ref.objectId.name);
        }
    }

    private void listAllTags()
    {
        List<Ref> call = new Git(repository).tagList().call()
        for (Ref ref : call)
        {
            ref.
            println('Tag: ' + ref.name + ' ' + ref.objectId.name)
        }
    }

    NCube loadCube(NCubeInfoDto cubeInfo, Integer revision)
    {
        ObjectId lastCommitId = repository.resolve(Constants.HEAD)

        final ApplicationID appId = cubeInfo.applicationID
        Repository repo = getRepoByAppId(appId)
        RevWalk walk = new RevWalk(repo)

        RevCommit commit = walk.parseCommit(lastCommitId)
        RevTree tree = commit.tree

        final String fname = cubeInfo.name + '.json'
        // Now use a TreeWalk to iterate over all files in the Tree recursively
        // you can set Filters to narrow down the results if needed
        TreeWalk treeWalk = new TreeWalk(repo)
        treeWalk.addTree(tree)
        TreeFilter filter = new TreeFilter() {
            boolean include(TreeWalk walker) throws MissingObjectException, IncorrectObjectTypeException, IOException
            {
                if (walker.subtree)
                {
                    return true;

                }
                return fname.equalsIgnoreCase(walker.nameString)
            }

            boolean shouldBeRecursive()
            {
                return true
            }

            TreeFilter clone()
            {
                return null
            }
        }
        treeWalk.filter = filter
        treeWalk.recursive = true
        NCube ncube = null

        while (treeWalk.next())
        {
            Matcher m = Regexes.gitCubeNames.matcher(treeWalk.pathString)
            if (m.find() && m.groupCount() > 0)
            {
                ObjectLoader loader = repository.open(treeWalk.getObjectId(0));
                InputStream input = loader.openStream();
                byte[] bytes = IOUtilities.inputStreamToBytes(input)
                IOUtilities.close(input)

                ncube = NCube.fromSimpleJson(StringUtilities.createString(bytes, 'UTF-8'))

            }
        }

        walk.dispose()
        if (ncube == null)
        {
            throw new IllegalArgumentException('Cube: ' + cubeInfo + ' not found.');
        }
        return ncube
    }

    Object[] getCubeRecords(ApplicationID appId, String pattern)
    {
        ObjectId lastCommitId = repository.resolve(Constants.HEAD)

        Repository repo = getRepoByAppId(appId)
        RevWalk walk = new RevWalk(repo)

        RevCommit commit = walk.parseCommit(lastCommitId)
        RevTree tree = commit.tree

        // Now use a TreeWalk to iterate over all files in the Tree recursively
        // you can set Filters to narrow down the results if needed
        TreeWalk treeWalk = new TreeWalk(repo)
        treeWalk.addTree(tree)
        treeWalk.recursive = true
        List<NCubeInfoDto> cubes = new ArrayList()

        while (treeWalk.next())
        {
            NCubeInfoDto info = new NCubeInfoDto()
            Matcher m = Regexes.gitCubeNames.matcher(treeWalk.pathString)
            if (m.find() && m.groupCount() > 0)
            {
                info.tenant = appId.tenant
                info.app = appId.app
                info.version = appId.version
                info.status = appId.status
                info.name = m.group(1)
                info.sha1 = treeWalk.getObjectId(0).name
                cubes.add(info)
            }
        }

        walk.dispose()
        return cubes.toArray()
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
//                revisions = getFileLog(git, treeWalk.pathString)
//                println revisions;
//                println '-------------------------'

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

    void createCube(ApplicationID appId, NCube cube, String username)
    {
    }

    void updateCube(ApplicationID appId, NCube cube, String username)
    {
    }

    boolean deleteCube(ApplicationID appId, String cubeName, boolean allowDelete, String username)
    {
        return false
    }

    boolean renameCube(ApplicationID appId, NCube oldCube, String newName)
    {
        return false
    }

    void restoreCube(ApplicationID appId, String cubeName, String username)
    {
    }

    boolean updateNotes(ApplicationID appId, String cubeName, String notes)
    {
        return false
    }

    int createSnapshotVersion(ApplicationID appId, String newVersion)
    {
        return 0
    }

    int changeVersionValue(ApplicationID appId, String newVersion)
    {
        return 0
    }

    int releaseCubes(ApplicationID appId)
    {
        return 0
    }

    boolean updateTestData(ApplicationID appId, String cubeName, String testData)
    {
        return false
    }

    static List getFileLog(Git git, String qualifiedFilename)
    {
        def logs = git.log().addPath(qualifiedFilename).call()
        def history = [];
        for (RevCommit rev : logs)
        {
            history.add([id: rev.id.name, comment: rev.fullMessage, revision:rev])
        }
        return history;
    }
}
