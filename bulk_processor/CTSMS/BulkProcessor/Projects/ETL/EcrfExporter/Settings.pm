package CTSMS::BulkProcessor::Projects::ETL::EcrfExporter::Settings;
use strict;

## no critic

use utf8;

use CTSMS::BulkProcessor::Globals qw(
    $enablemultithreading
    $cpucount
);


use CTSMS::BulkProcessor::Logging qw(
    getlogger
    scriptinfo
    configurationinfo
);

use CTSMS::BulkProcessor::LogError qw(
    fileerror
    configurationwarn
    configurationerror
);

use CTSMS::BulkProcessor::LoadConfig qw(
    split_tuple
    parse_regexp
);
use CTSMS::BulkProcessor::Utils qw(format_number prompt chopstring); #check_ipnet

#use CTSMS::BulkProcessor::RestRequests::ctsms::trial::TrialService::Ecrf qw();
use CTSMS::BulkProcessor::RestRequests::ctsms::trial::TrialService::Trial qw();

require Exporter;
our @ISA = qw(Exporter);
our @EXPORT_OK = qw(
    update_settings

    $defaultsettings
    $defaultconfig

    $force



);

our $defaultconfig = 'config.cfg';
our $defaultsettings = 'settings.yml';

our $force = 0;




sub update_settings {

    my ($data,$configfile) = @_;

    if (defined $data) { # and defined ($data = $data->[0])) {

        my $result = 1;


        return $result;

    }
    return 0;

}

1;
