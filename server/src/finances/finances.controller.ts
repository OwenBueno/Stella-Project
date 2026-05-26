import { Body, Controller, Get, Param, Patch, Post, Query } from '@nestjs/common';
import { CreateDebtDto, CreatePenaltyDto, UpdateDebtDto } from './dto/create-debt.dto';
import { CreateTransactionDto } from './dto/create-transaction.dto';
import { FinancesService } from './finances.service';

@Controller('finances')
export class FinancesController {
  constructor(private readonly financesService: FinancesService) {}

  @Post('transactions')
  createTransaction(@Body() dto: CreateTransactionDto) {
    return this.financesService.createTransaction(dto);
  }

  @Get('transactions')
  listTransactions(
    @Query('type') type?: string,
    @Query('category') category?: string,
    @Query('startDate') startDate?: string,
    @Query('endDate') endDate?: string,
  ) {
    return this.financesService.listTransactions(type, category, startDate, endDate);
  }

  @Post('debts')
  createDebt(@Body() dto: CreateDebtDto) {
    return this.financesService.createDebt(dto);
  }

  @Patch('debts/:id')
  updateDebt(@Param('id') id: string, @Body() dto: UpdateDebtDto) {
    return this.financesService.updateDebt(id, dto);
  }

  @Get('debts')
  listDebts(@Query('resolved') resolved?: string) {
    return this.financesService.listDebts(resolved);
  }

  @Get('summary')
  getSummary(
    @Query('year') year: string,
    @Query('month') month: string,
  ) {
    return this.financesService.getSummary(Number(year), Number(month));
  }

  @Post('penalties')
  createPenalty(@Body() dto: CreatePenaltyDto) {
    return this.financesService.createPenaltyEgress(dto);
  }
}
