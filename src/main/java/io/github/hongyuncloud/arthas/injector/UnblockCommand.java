package io.github.hongyuncloud.arthas.injector;

import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Summary;
import io.github.hongyuncloud.arthas.helper.LockableThreadHelper;

@Name("continue")
@Summary("continue process server")
@Description(Constants.EXAMPLE +
    "  unblock-server")
public class UnblockCommand extends AnnotatedCommand {
  private String lockName;

  @Argument(argName = "lock-name", index = 0, required = false)
  @Description("lock name")
  public void setLockName(final String lockName) {
    this.lockName = lockName;
  }

  @Override
  public void process(CommandProcess process) {
    LockableThreadHelper.exitLock(lockName);
    process.write("unblock success\n");
    process.end();
  }
}
