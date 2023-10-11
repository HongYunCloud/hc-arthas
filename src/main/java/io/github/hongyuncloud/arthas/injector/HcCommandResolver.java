package io.github.hongyuncloud.arthas.injector;

import com.taobao.arthas.core.command.BuiltinCommandPack;
import com.taobao.arthas.core.config.Configure;
import com.taobao.arthas.core.server.ArthasBootstrap;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.Command;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.middleware.cli.annotations.Name;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class HcCommandResolver extends BuiltinCommandPack {
  private List<Command> commands = new ArrayList<>();

  public HcCommandResolver() {
    super(Collections.emptyList());
    final Configure configure = ArthasBootstrap.getInstance().getConfigure();
    final List<String> disabledCommands = new ArrayList<String>();
    if (configure.getDisabledCommands() != null) {
      final String[] strings = StringUtils.tokenizeToStringArray(configure.getDisabledCommands(), ",");
      if (strings != null) {
        disabledCommands.addAll(Arrays.asList(strings));
      }
    }
    initCommands(disabledCommands);
  }

  @Override
  public List<Command> commands() {
    return commands;
  }

  private void initCommands(List<String> disabledCommands) {
    List<Class<? extends AnnotatedCommand>> commandClassList = new ArrayList<Class<? extends AnnotatedCommand>>(33);

    commandClassList.add(UnblockCommand.class);

    for (Class<? extends AnnotatedCommand> clazz : commandClassList) {
      Name name = clazz.getAnnotation(Name.class);
      if (name != null && name.value() != null) {
        if (disabledCommands.contains(name.value())) {
          continue;
        }
      }
      commands.add(Command.create(clazz));
    }
  }
}
