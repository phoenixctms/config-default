use strict;

## no critic

use File::Basename;
use Cwd;
use lib Cwd::abs_path(File::Basename::dirname(__FILE__) . '/../../../../../');

use Getopt::Long qw(GetOptions);
use Fcntl qw(LOCK_EX LOCK_NB);

use CTSMS::BulkProcessor::Globals qw();
use CTSMS::BulkProcessor::Projects::ETL::EcrfSettings qw(
    $output_path
    $skip_errors
    $ctsms_base_url
    $ecrf_data_trial_id
);
use CTSMS::BulkProcessor::Projects::ETL::EcrfExporter::Settings qw(
    $defaultsettings
    $defaultconfig
    $force
);
use CTSMS::BulkProcessor::Logging qw(
    init_log
    getlogger
    $attachmentlogfile
    scriptinfo
    cleanuplogfiles
    $currentlogfile
);
use CTSMS::BulkProcessor::LogError qw (
    completion
    done
    scriptwarn
    scripterror
    filewarn
    fileerror
);
use CTSMS::BulkProcessor::LoadConfig qw(
    load_config
    $SIMPLE_CONFIG_TYPE
    $YAML_CONFIG_TYPE
    $ANY_CONFIG_TYPE
);
use CTSMS::BulkProcessor::Array qw(removeduplicates);
use CTSMS::BulkProcessor::Utils qw(getscriptpath prompt cleanupdir);
use CTSMS::BulkProcessor::Mail qw(
    cleanupmsgfiles
);
use CTSMS::BulkProcessor::SqlConnectors::CSVDB qw(cleanupcvsdirs);
use CTSMS::BulkProcessor::SqlConnectors::SQLiteDB qw(cleanupdbfiles);

use CTSMS::BulkProcessor::Projects::ETL::EcrfConnectorPool qw(destroy_all_dbs);
#use CTSMS::BulkProcessor::ConnectorPool qw(destroy_dbs);

use CTSMS::BulkProcessor::Projects::ETL::EcrfExport qw(
    export_ecrf_data_vertical
    export_ecrf_data_horizontal

    publish_ecrf_data_sqlite
    publish_ecrf_data_horizontal_csv
    publish_ecrf_data_xls
    publish_ecrf_data_pdf
    publish_ecrf_data_pdfs

    publish_audit_trail_xls
    publish_ecrf_journal_xls
    publish_ecrfs_xls
);

scripterror(getscriptpath() . ' already running',getlogger(getscriptpath())) unless flock DATA, LOCK_EX | LOCK_NB; # not tested on windows yet

my @TASK_OPTS = ();

my $tasks = [];

my $cleanup_task_opt = 'cleanup';
push(@TASK_OPTS,$cleanup_task_opt);

my $cleanup_all_task_opt = 'cleanup_all';
push(@TASK_OPTS,$cleanup_all_task_opt);

my $export_ecrf_data_vertical_task_opt = 'export_ecrf_data_vertical';
push(@TASK_OPTS,$export_ecrf_data_vertical_task_opt);

my $export_ecrf_data_horizontal_task_opt = 'export_ecrf_data_horizontal';
push(@TASK_OPTS,$export_ecrf_data_horizontal_task_opt);

my $publish_ecrf_data_sqlite_task_opt = 'publish_ecrf_data_sqlite';
push(@TASK_OPTS,$publish_ecrf_data_sqlite_task_opt);

my $publish_ecrf_data_horizontal_csv_task_opt = 'publish_ecrf_data_horizontal_csv';
push(@TASK_OPTS,$publish_ecrf_data_horizontal_csv_task_opt);

my $publish_ecrf_data_xls_task_opt = 'publish_ecrf_data_xls';
push(@TASK_OPTS,$publish_ecrf_data_xls_task_opt);

my $publish_ecrf_data_pdf_task_opt = 'publish_ecrf_data_pdf';
push(@TASK_OPTS,$publish_ecrf_data_pdf_task_opt);

my $publish_ecrf_data_pdfs_task_opt = 'publish_ecrf_data_pdfs';
push(@TASK_OPTS,$publish_ecrf_data_pdfs_task_opt);

my $publish_audit_trail_xls_task_opt = 'publish_audit_trail_xls';
push(@TASK_OPTS,$publish_audit_trail_xls_task_opt);

my $publish_ecrf_journal_xls_task_opt = 'publish_ecrf_journal_xls';
push(@TASK_OPTS,$publish_ecrf_journal_xls_task_opt);

my $publish_ecrfs_xls_task_opt = 'publish_ecrfs_xls';
push(@TASK_OPTS,$publish_ecrfs_xls_task_opt);

if (init()) {
    main();
    exit(0);
} else {
    exit(1);
}

sub init {

    my $configfile = $defaultconfig;
    my $settingsfile = $defaultsettings;
    #print STDERR (join("|",@ARGV),"\n");
    return 0 unless GetOptions(
        "config=s" => \$configfile,
        "settings=s" => \$settingsfile,
        "task=s" => $tasks,
        "skip-errors" => \$skip_errors,
        "force" => \$force,
        "id=i" => \$ecrf_data_trial_id,
    ); # or scripterror('error in command line arguments',getlogger(getscriptpath()));

    $tasks = removeduplicates($tasks,1);

    my $result = load_config($configfile);
    init_log();
    $result &= load_config($settingsfile,\&CTSMS::BulkProcessor::Projects::ETL::EcrfSettings::update_settings,$YAML_CONFIG_TYPE);
    $result &= load_config($settingsfile,\&CTSMS::BulkProcessor::Projects::ETL::EcrfExporter::Settings::update_settings,$YAML_CONFIG_TYPE);
    #$result &= load_config($some_yml,\&update_something,$YAML_CONFIG_TYPE);
    return $result;

}

sub main() {

    my @messages = ( 'Trial eCRF export/data aggregation:' );
    my @attachmentfiles = ();
    my $result = 1;
    my $completion = 0;

    if (defined $tasks and 'ARRAY' eq ref $tasks and (scalar @$tasks) > 0) {
        #scriptinfo('skip-errors: processing won\'t stop upon errors',getlogger(__PACKAGE__)) if $skip_errors;
        foreach my $task (@$tasks) {

            if (lc($cleanup_task_opt) eq lc($task)) {
                $result &= cleanup_task(\@messages,0) if taskinfo($cleanup_task_opt,\$result);

            } elsif (lc($cleanup_all_task_opt) eq lc($task)) {
                $result &= cleanup_task(\@messages,1) if taskinfo($cleanup_all_task_opt,\$result);

            } elsif (lc($export_ecrf_data_vertical_task_opt) eq lc($task)) {
                $result &= export_ecrf_data_vertical_task(\@messages) if taskinfo($export_ecrf_data_vertical_task_opt,\$result,1);
            } elsif (lc($export_ecrf_data_horizontal_task_opt) eq lc($task)) {
                $result &= export_ecrf_data_horizontal_task(\@messages) if taskinfo($export_ecrf_data_horizontal_task_opt,\$result,1);

            } elsif (lc($publish_ecrf_data_sqlite_task_opt) eq lc($task)) {
                $result &= publish_ecrf_data_sqlite_task(\@messages,\@attachmentfiles) if taskinfo($publish_ecrf_data_sqlite_task_opt,\$result,1);
                $completion = $result;
            } elsif (lc($publish_ecrf_data_horizontal_csv_task_opt) eq lc($task)) {
                $result &= publish_ecrf_data_horizontal_csv_task(\@messages,\@attachmentfiles) if taskinfo($publish_ecrf_data_horizontal_csv_task_opt,\$result,1);
                $completion = $result;
            } elsif (lc($publish_ecrf_data_xls_task_opt) eq lc($task)) {
                $result &= publish_ecrf_data_xls_task(\@messages,\@attachmentfiles) if taskinfo($publish_ecrf_data_xls_task_opt,\$result,1);
                $completion = $result;
            } elsif (lc($publish_ecrf_data_pdf_task_opt) eq lc($task)) {
                $result &= publish_ecrf_data_pdf_task(\@messages,\@attachmentfiles) if taskinfo($publish_ecrf_data_pdf_task_opt,\$result,1);
                $completion = $result;
            } elsif (lc($publish_ecrf_data_pdfs_task_opt) eq lc($task)) {
                $result &= publish_ecrf_data_pdfs_task(\@messages,\@attachmentfiles) if taskinfo($publish_ecrf_data_pdfs_task_opt,\$result,1);
                $completion = $result;

            } elsif (lc($publish_audit_trail_xls_task_opt) eq lc($task)) {
                $result &= publish_audit_trail_xls_task(\@messages,\@attachmentfiles) if taskinfo($publish_audit_trail_xls_task_opt,\$result,1);
                $completion = $result;
            } elsif (lc($publish_ecrf_journal_xls_task_opt) eq lc($task)) {
                $result &= publish_ecrf_journal_xls_task(\@messages,\@attachmentfiles) if taskinfo($publish_ecrf_journal_xls_task_opt,\$result,1);
                $completion = $result;
            } elsif (lc($publish_ecrfs_xls_task_opt) eq lc($task)) {
                $result &= publish_ecrfs_xls_task(\@messages,\@attachmentfiles) if taskinfo($publish_ecrfs_xls_task_opt,\$result,1);
                $completion = $result;

            } else {
                $result = 0;
                scripterror("unknow task option '" . $task . "', must be one of " . join(', ',@TASK_OPTS),getlogger(getscriptpath()));
                last;
            }
        }
        destroy_all_dbs();
    } else {
        $result = 0;
        scripterror('at least one task option is required. supported tasks: ' . join(', ',@TASK_OPTS),getlogger(getscriptpath()));
    }

    push(@attachmentfiles,$attachmentlogfile);
    if ($result and $completion) {
        push(@messages,"Visit $ctsms_base_url/trial/trial.jsf?trialid=$ecrf_data_trial_id to download files.");
        completion(join("\n\n",@messages),\@attachmentfiles,getlogger(getscriptpath()));
    } elsif ($result) {
        done(join("\n\n",@messages),\@attachmentfiles,getlogger(getscriptpath()));
    } else {
        scriptwarn(join("\n\n",@messages),getlogger(getscriptpath()),1);
    }

    return $result;
}

sub taskinfo {
    my ($task,$result_ref,$ecrf_data_trial_id_required) = @_;
    scriptinfo($$result_ref ? "starting task: '$task'" : "skipping task '$task' due to previous problems",getlogger(getscriptpath()));
    if ($ecrf_data_trial_id_required and (not defined $ecrf_data_trial_id or length($ecrf_data_trial_id) == 0)) {
        scripterror("trial id required",getlogger(getscriptpath()));
        $$result_ref = 0;
    }
    return $$result_ref;
}

sub cleanup_task {
    my ($messages,$clean_generated) = @_;
    my $result = 0;
    if (!$clean_generated or $force or 'yes' eq lc(prompt("Type 'yes' to proceed: "))) {
        eval {
            cleanupcvsdirs() if $clean_generated;
            cleanupdbfiles() if $clean_generated;
            cleanuplogfiles(\&fileerror,\&filewarn,($currentlogfile,$attachmentlogfile));
            cleanupmsgfiles(\&fileerror,\&filewarn);
            cleanupdir($output_path,1,\&filewarn,getlogger(getscriptpath())) if $clean_generated;
            $result = 1;
        };
    }
    if ($@ or !$result) {
        push(@$messages,'working directory cleanup error');
        return 0;
    } else {
        push(@$messages,'- working directory folders cleaned up');
        return 1;
    }
}

sub export_ecrf_data_vertical_task {
    my ($messages) = @_;
    my ($result, $warning_count) = (0,0);
    eval {
        ($result, $warning_count) = export_ecrf_data_vertical();
    };
    my $err = $@;

    if ($err) {
        #print $@;
        push(@$messages,'export_ecrf_data_vertical error: ' . $err);
        return 0;
    } else {
        push(@$messages,'- vertical eCRF data prepared');
        return 1;
    }
}

sub export_ecrf_data_horizontal_task {
    my ($messages) = @_;
    my ($result, $warning_count) = (0,0);
    eval {
        ($result, $warning_count) = export_ecrf_data_horizontal();
    };
    my $err = $@;

    if ($err) {
        #print $@;
        push(@$messages,'export_ecrf_data_horizontal error: ' . $err);
        return 0;
    } else {
        push(@$messages,'- horizontal eCRF data prepared');
        return 1;
    }
}

sub publish_ecrf_data_sqlite_task {
    my ($messages,$attachmentfiles) = @_;
    my ($out,$filename) = (undef,undef);
    eval {
        ($out,$filename) = publish_ecrf_data_sqlite();
        #push(@$attachmentfiles,$filename);
    };
    my $err = $@;
    $err ||= 'no file created' unless $out;
    if ($err) {
        #print $@;
        push(@$messages,'publish_ecrf_data_sqlite error: ' . $err);
        return 0;
    } else {
        push(@$messages,"- file '$out->{title}' added to the '$out->{trial}->{name}' trial");
        return 1;
    }
}

sub publish_ecrf_data_horizontal_csv_task {
    my ($messages,$attachmentfiles) = @_;
    my ($out,$filename) = (undef,undef);
    eval {
        ($out,$filename) = publish_ecrf_data_horizontal_csv();
        #push(@$attachmentfiles,$filename);
    };
    my $err = $@;
    $err ||= 'no file created' unless $out;
    if ($err) {
        #print $@;
        push(@$messages,'publish_ecrf_data_horizontal_csv error: ' . $err);
        return 0;
    } else {
        push(@$messages,"- file '$out->{title}' added to the '$out->{trial}->{name}' trial");
        return 1;
    }
}

sub publish_ecrf_data_xls_task {
    my ($messages,$attachmentfiles) = @_;
    my ($out,$filename) = (undef,undef);
    eval {
        ($out,$filename) = publish_ecrf_data_xls();
        #push(@$attachmentfiles,$filename);
    };
    my $err = $@;
    $err ||= 'no file created' unless $out;
    if ($err) {
        #print $@;
        push(@$messages,'publish_ecrf_data_xls error: ' . $err);
        return 0;
    } else {
        push(@$messages,"- file '$out->{title}' added to the '$out->{trial}->{name}' trial");
        return 1;
    }
}

sub publish_ecrf_data_pdf_task {
    my ($messages,$attachmentfiles) = @_;
    my ($out,$filename) = (undef,undef);
    eval {
        ($out,$filename) = publish_ecrf_data_pdf();
        #push(@$attachmentfiles,$filename);
    };
    my $err = $@;
    $err ||= 'no file created' unless $out;
    if ($err) {
        #print $@;
        push(@$messages,'publish_ecrf_data_pdf error: ' . $err);
        return 0;
    } else {
        push(@$messages,"- file '$out->{title}' added to the '$out->{trial}->{name}' trial");
        return 1;
    }
}

sub publish_ecrf_data_pdfs_task {
    my ($messages,$attachmentfiles) = @_;
    my ($result, $warning_count, $uploads) = (0,0,undef);
    eval {
        ($result, $warning_count, $uploads) = publish_ecrf_data_pdfs();
    };
    my $err = $@;
    $err ||= 'no files downloaded' unless ('ARRAY' eq ref $uploads and (scalar @$uploads > 0));
    if ($err) {
        #print $@;
        push(@$messages,'publish_ecrf_data_pdfs error: ' . $err);
        return 0;
    } else {
        foreach my $upload (@$uploads) {
            my ($out,$filename) = @$upload;
            push(@$messages,"- file '$out->{title}' added to the '$out->{trial}->{name}' trial");
        }
        return 1;
    }
}

sub publish_audit_trail_xls_task {
    my ($messages,$attachmentfiles) = @_;
    my ($out,$filename) = (undef,undef);
    eval {
        ($out,$filename) = publish_audit_trail_xls();
        #push(@$attachmentfiles,$filename);
    };
    my $err = $@;
    $err ||= 'no file created' unless $out;
    if ($err) {
        #print $@;
        push(@$messages,'publish_audit_trail_xls error: ' . $err);
        return 0;
    } else {
        push(@$messages,"- file '$out->{title}' added to the '$out->{trial}->{name}' trial");
        return 1;
    }
}

sub publish_ecrf_journal_xls_task {
    my ($messages,$attachmentfiles) = @_;
    my ($out,$filename) = (undef,undef);
    eval {
        ($out,$filename) = publish_ecrf_journal_xls();
        #push(@$attachmentfiles,$filename);
    };
    my $err = $@;
    $err ||= 'no file created' unless $out;
    if ($err) {
        #print $@;
        push(@$messages,'publish_ecrf_journal_xls error: ' . $err);
        return 0;
    } else {
        push(@$messages,"- file '$out->{title}' added to the '$out->{trial}->{name}' trial");
        return 1;
    }
}

sub publish_ecrfs_xls_task {
    my ($messages,$attachmentfiles) = @_;
    my ($out,$filename) = (undef,undef);
    eval {
        ($out,$filename) = publish_ecrfs_xls();
        #push(@$attachmentfiles,$filename);
    };
    my $err = $@;
    $err ||= 'no file created' unless $out;
    if ($err) {
        #print $@;
        push(@$messages,'publish_ecrfs_xls error: ' . $err);
        return 0;
    } else {
        push(@$messages,"- file '$out->{title}' added to the '$out->{trial}->{name}' trial");
        return 1;
    }
}

#END {
#    # this should not be required explicitly, but prevents Log4Perl's
#    # "rootlogger not initialized error upon exit..
#    destroy_all_dbs
#}

__DATA__
This exists to allow the locking code at the beginning of the file to work.
DO NOT REMOVE THESE LINES!
