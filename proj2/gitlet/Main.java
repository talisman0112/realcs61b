package gitlet;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author TODO
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ...
     */
    public static void main(String[] args) {
        // TODO: what if args is empty?
        String firstArg = args[0];
        Repository repo=new Repository();
        switch(firstArg) {
            case "init":
                // TODO: handle the `init` command
                repo.init();
                break;
            case "add":
                // TODO: handle the `add [filename]` command
                if (args.length != 2) {
                    throw new GitletException("Incorrect operands.");
                    }
                repo.add(args[1]);
                break;
            case "log":
                if (args.length != 1) {
                    throw new GitletException("Incorrect operands.");
                }
                repo.log();
                break;
            case "rm":
                if (args.length != 2) {
                    throw new GitletException("Incorrect operands.");
                }
                repo.rm(args[1]);
                break;
            case"commit":
                if (args.length != 2) {
                    throw new GitletException("Incorrect operands.");
                }
                repo.commit(args[1]);
                break;
            case"global-log":
                if (args.length != 1) {
                    throw new GitletException("Incorrect operands.");
                }
                repo.globalLog();
                break;
            case"find":
                if (args.length != 2) {
                    throw new GitletException("Incorrect operands.");
                }
                repo.find(args[1]);
                break;
            case"status":
                if (args.length != 1) {
                    throw new GitletException("Incorrect operands.");
                }
                repo.status();
                break;
            case"branch":
                if (args.length != 2) {
                    throw new GitletException("Incorrect operands.");
                }
                repo.branch(args[1]);
                break;
            case "checkout":
                checkoutHandler(args, repo);
                break;
            case"rm-branch":
                if (args.length != 2) {
                    throw new GitletException("Incorrect operands.");
                }
                repo.rm_branch(args[1]);
                break;
            case"reset":
                if (args.length != 2) {
                    throw new GitletException("Incorrect operands.");
                }
                repo.reset(args[1]);
                break;
            case"merge":
                if (args.length != 2) {
                    throw new GitletException("Incorrect operands.");
                }
                repo.merge(args[1]);
                break;
            // TODO: FILL THE REST IN
        }
    }
    private static void checkoutHandler(String[] args, Repository repo) {
        if (args.length == 3 && "--".equals(args[1])) {
            // java gitlet.Main checkout -- filename
            repo.checkoutFile(args[2]);
        } else if (args.length == 4 && "--".equals(args[2])) {
            // java gitlet.Main checkout commitId -- filename
            repo.checkoutFile(args[1], args[3]);
        } else if (args.length == 2) {
            // java gitlet.Main checkout branchName
            repo.checkoutBranch(args[1]);
        } else {
            throw new GitletException("Incorrect operands.");
        }
    }
}